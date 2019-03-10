package com.cognifide.gradle.aem.common

import com.cognifide.gradle.aem.bundle.BundlePlugin
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.http.HttpClient
import com.cognifide.gradle.aem.config.Config
import com.cognifide.gradle.aem.config.ConfigPlugin
import com.cognifide.gradle.aem.instance.*
import com.cognifide.gradle.aem.pkg.PackageDefinition
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.pkg.tasks.Compose
import com.cognifide.gradle.aem.pkg.vlt.VltFilter
import com.cognifide.gradle.aem.tooling.ToolingPlugin
import com.cognifide.gradle.aem.tooling.vlt.VltException
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File
import java.time.ZoneId
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested

@Suppress("TooManyFunctions")
open class AemExtension(@Internal val project: Project) {

    @Internal
    val logger = project.logger

    /**
     * Allows to read project property specified in command line and system property as a fallback.
     */
    @Internal
    val props = PropertyParser(this)

    /**
     * Project under which common configuration files are stored.
     * Usually it is also a project which is building full assembly CRX package.
     *
     * Convention assumes in case of:
     * - multi-project build - subproject with path ':aem'
     * - single-project build - root project
     */
    @get:Internal
    @get:JsonIgnore
    val projectMain: Project = project.findProject(props.string("aem.projectMainPath") ?: ":aem") ?: project.rootProject

    /**
     * Project name convention prefixes used to determine default:
     *
     * - bundle install subdirectory
     * - CRX package base name
     * - OSGi bundle JAR base name
     *
     * in case of multi-project build and assembly packages.
     */
    @get:Internal
    @get:JsonIgnore
    val projectPrefixes: List<String> = props.list("aem.projectPrefixes") ?: listOf("aem.", "aem-", "aem_")

    /**
     * Project name with skipped convention prefixes.
     */
    @get:Internal
    @get:JsonIgnore
    val projectName: String
        get() = project.name.run {
            var n = this; projectPrefixes.forEach { n = n.removePrefix(it) }; n
        }

    /**
     * Base name used as default for CRX packages being created by compose or collect task
     * and also for OSGi bundle JARs.
     */
    @get:Internal
    @get:JsonIgnore
    val baseName: String
        get() = Formats.normalizeSeparators(if (project == project.rootProject) {
            project.rootProject.name
        } else {
            "${project.rootProject.name}.$projectName"
        }, ".")

    /**
     * Determines current environment to be used in e.g package deployment.
     */
    @Input
    val environment: String = props.string("aem.env") ?: run { System.getenv("AEM_ENV") ?: "local" }

    /**
     * Timezone ID (default for defined instances)
     */
    @Internal
    @JsonIgnore
    var zoneId: ZoneId = props.string("aem.zoneId")?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()

    /**
     * Performs parallel CRX package deployments and instance synchronization.
     */
    @Internal
    val parallel = ParallelExecutor(this)

    /**
     * Collection of common AEM configuration properties like instance definitions. Contains default values for tasks.
     */
    @Nested
    val config = Config(this)

    /**
     * Directory for storing project specific files used by plugin e.g:
     * - Groovy Scripts to be launched by instance sync in tasks defined in project
     */
    @get:Internal
    val configDir: File
        get() = project.file(props.string("aem.configDir") ?: "gradle")

    /**
     * Directory for storing common files used by plugin e.g:
     * - CRX package thumbnail
     * - tail incident filter
     */
    @get:Internal
    val configCommonDir: File
        get() = projectMain.file(props.string("aem.configDir") ?: "gradle")

    /**
     * Provides API for displaying interactive notification during running build tasks.
     */
    @Internal
    val notifier = NotifierFacade.of(this)

    /**
     * Provides API for easier creation of tasks (e.g in sequence) in the matter of Gradle task configuration avoidance.
     */
    @Internal
    val tasks = TaskFacade(this)

    /**
     * Provides API for performing actions affecting multiple instances at once.
     */
    @Internal
    val actions = ActionPerformer(this)

    /**
     * Collection of all java packages from all projects applying bundle plugin.
     */
    @get:Internal
    val javaPackages: List<String>
        get() = AemPlugin.withId(project, BundlePlugin.ID).flatMap { subproject ->
            AemExtension.of(subproject).tasks.bundles.mapNotNull { it.javaPackage }
        }

    @get:Internal
    val instances: List<Instance>
        get() = filterInstances()

    fun instances(consumer: (Instance) -> Unit) = parallel.with(instances, consumer)

    fun instances(filter: String, consumer: (Instance) -> Unit) = parallel.with(filterInstances(filter), consumer)

    fun instance(urlOrName: String): Instance = config.parseInstance(urlOrName)

    fun instances(urlsOrNames: Collection<String>): List<Instance> = urlsOrNames.map { instance(it) }

    @get:Internal
    val anyInstance: Instance
        get() {
            val cmdInstanceArg = props.string("aem.instance")
            if (!cmdInstanceArg.isNullOrBlank()) {
                return instance(cmdInstanceArg)
            }

            return namedInstance(Instance.FILTER_ANY)
        }

    fun namedInstance(desiredName: String? = props.string("aem.instance.name"), defaultName: String = "$environment-*"): Instance {
        val nameMatcher: String = desiredName ?: defaultName

        val namedInstance = filterInstances(nameMatcher).firstOrNull()
        if (namedInstance != null) {
            return namedInstance
        }

        throw InstanceException("Instance named '$nameMatcher' is not defined.")
    }

    fun filterInstances(nameMatcher: String = props.string("aem.instance.name") ?: "$environment-*"): List<Instance> {
        val all = config.instances.values

        // Specified by command line should not be filtered
        val cmd = all.filter { it.environment == Instance.ENVIRONMENT_CMD }
        if (cmd.isNotEmpty()) {
            return cmd
        }

        // Defined by build script, via properties or defaults are filterable by name
        return all.filter { instance ->
            when {
                props.flag("aem.instance.authors") -> {
                    Patterns.wildcard(instance.name, "$environment-${InstanceType.AUTHOR}*")
                }
                props.flag("aem.instance.publishers") -> {
                    Patterns.wildcard(instance.name, "$environment-${InstanceType.PUBLISH}*")
                }
                else -> Patterns.wildcard(instance.name, nameMatcher)
            }
        }
    }

    @get:Internal
    val authorInstances: List<Instance>
        get() = filterInstances().filter { it.type == InstanceType.AUTHOR }

    fun authorInstances(consumer: (Instance) -> Unit) = parallel.with(authorInstances, consumer)

    @get:Internal
    val publishInstances: List<Instance>
        get() = filterInstances().filter { it.type == InstanceType.PUBLISH }

    fun publishInstances(consumer: Instance.() -> Unit) = parallel.with(publishInstances, consumer)

    @get:Internal
    val localInstances: List<LocalInstance>
        get() = instances.filterIsInstance(LocalInstance::class.java)

    fun localInstances(consumer: LocalInstance.() -> Unit) = parallel.with(localInstances, consumer)

    @get:Internal
    val remoteInstances: List<RemoteInstance>
        get() = instances.filterIsInstance(RemoteInstance::class.java)

    fun remoteInstances(consumer: RemoteInstance.() -> Unit) = parallel.with(remoteInstances, consumer)

    fun packages(consumer: (File) -> Unit) = parallel.with(packages, consumer)

    @get:Internal
    val packages: List<File>
        get() = project.tasks.withType(Compose::class.java)
                .map { it.archivePath }

    fun packagesDependent(task: Task): List<File> {
        return task.taskDependencies.getDependencies(task)
                .filterIsInstance(Compose::class.java)
                .map { it.archivePath }
    }

    fun sync(synchronizer: InstanceSync.() -> Unit) = sync(instances, synchronizer)

    fun sync(instances: Collection<Instance>, synchronizer: InstanceSync.() -> Unit) {
        parallel.with(instances) { this.sync.apply(synchronizer) }
    }

    fun syncPackages(synchronizer: InstanceSync.(File) -> Unit) = syncPackages(instances, packages, synchronizer)

    fun syncPackages(
        instances: Collection<Instance>,
        packages: Collection<File>,
        synchronizer: InstanceSync.(File) -> Unit
    ) {
        packages.forEach { pkg -> // single AEM instance dislikes parallel package installation
            parallel.with(instances) { // but same package could be in parallel deployed on different AEM instances
                sync.apply { synchronizer(pkg) }
            }
        }
    }

    fun composePackage(definition: PackageDefinition.() -> Unit): File {
        return PackageDefinition(this).compose(definition)
    }

    fun <T> http(consumer: HttpClient.() -> T) = HttpClient(this).run(consumer)

    fun config(configurer: Config.() -> Unit) {
        config.apply(configurer)
    }

    fun notifier(configurer: NotifierFacade.() -> Unit) {
        notifier.apply(configurer)
    }

    fun tasks(configurer: TaskFacade.() -> Unit) {
        tasks.apply(configurer)
    }

    fun retry(configurer: Retry.() -> Unit): Retry {
        return retry().apply(configurer)
    }

    fun retry(): Retry = Retry.none(this)

    /**
     * Show asynchronous progress indicator with percentage while performing some action.
     */
    fun <T> progress(total: Int, action: ProgressIndicator.() -> T): T = progress(total.toLong(), action)

    /**
     * Show asynchronous progress indicator with percentage while performing some action.
     */
    fun <T> progress(total: Long, action: ProgressIndicator.() -> T): T {
        return ProgressIndicator(project).apply { this.total = total }.launch(action)
    }

    /**
     * Show asynchronous progress indicator while performing some action.
     *
     * Warning! Nesting progress indicators is not supported.
     */
    fun <T> progressIndicator(action: ProgressIndicator.() -> T): T = ProgressIndicator(project).launch(action)

    /**
     * Show synchronous progress logger while performing some action.
     *
     * Nesting progress loggers is supported.
     */
    fun <T> progressLogger(action: ProgressLogger.() -> T): T = ProgressLogger.of(project).launch(action)

    @get:Internal
    val filter: VltFilter
        get() {
            val cmdFilterRoots = props.list("aem.filter.roots") ?: listOf()
            if (cmdFilterRoots.isNotEmpty()) {
                logger.debug("Using Vault filter roots specified as command line property: $cmdFilterRoots")
                return VltFilter.temporary(project, cmdFilterRoots)
            }

            val cmdFilterPath = props.string("aem.filter.path") ?: ""
            if (cmdFilterPath.isNotEmpty()) {
                val cmdFilter = FileOperations.find(project, config.packageVltRoot, cmdFilterPath)
                        ?: throw VltException("Vault check out filter file does not exist at path: $cmdFilterPath" +
                                " (or under directory: ${config.packageVltRoot}).")
                logger.debug("Using Vault filter file specified as command line property: $cmdFilterPath")
                return VltFilter(cmdFilter)
            }

            val conventionFilterFiles = listOf(
                    "${config.packageVltRoot}/${VltFilter.CHECKOUT_NAME}",
                    "${config.packageVltRoot}/${VltFilter.BUILD_NAME}"
            )
            val conventionFilterFile = FileOperations.find(project, config.packageVltRoot, conventionFilterFiles)
            if (conventionFilterFile != null) {
                logger.debug("Using Vault filter file found by convention: $conventionFilterFile")
                return VltFilter(conventionFilterFile)
            }

            logger.debug("None of Vault filter files found by CMD properties or convention.")

            return VltFilter.temporary(project, listOf())
        }

    fun filter(file: File) = VltFilter(file)

    fun filter(path: String) = filter(project.file(path))

    fun temporaryDir(task: Task) = temporaryDir(task.name)

    fun temporaryDir(name: String) = AemTask.temporaryDir(project, name)

    fun temporaryFile(name: String) = AemTask.temporaryFile(project, TEMPORARY_DIR, name)

    @get:Internal
    val temporaryDir: File
        get() = temporaryDir(TEMPORARY_DIR)

    companion object {

        const val NAME = "aem"

        const val TEMPORARY_DIR = "tmp"

        private val PLUGIN_IDS = listOf(
                PackagePlugin.ID,
                BundlePlugin.ID,
                InstancePlugin.ID,
                ToolingPlugin.ID,
                ConfigPlugin.ID
        )

        fun of(project: Project): AemExtension {
            return project.extensions.findByType(AemExtension::class.java)
                    ?: throw AemException("${project.displayName.capitalize()} must have at least one of following plugins applied: $PLUGIN_IDS")
        }
    }
}