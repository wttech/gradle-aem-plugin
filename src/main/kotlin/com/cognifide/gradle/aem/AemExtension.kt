package com.cognifide.gradle.aem

import com.cognifide.gradle.aem.bundle.BundlePlugin
import com.cognifide.gradle.aem.common.CommonPlugin
import com.cognifide.gradle.aem.common.build.*
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.file.FileWatcher
import com.cognifide.gradle.aem.common.file.resolver.ResolverOptions
import com.cognifide.gradle.aem.common.file.transfer.FileMultiTransfer
import com.cognifide.gradle.aem.common.file.transfer.FileTransfer
import com.cognifide.gradle.aem.common.http.HttpClient
import com.cognifide.gradle.aem.common.instance.*
import com.cognifide.gradle.aem.common.notifier.NotifierFacade
import com.cognifide.gradle.aem.common.pkg.PackageDefinition
import com.cognifide.gradle.aem.common.pkg.PackageOptions
import com.cognifide.gradle.aem.common.pkg.vlt.VltFilter
import com.cognifide.gradle.aem.common.utils.Formats
import com.cognifide.gradle.aem.common.utils.LineSeparator
import com.cognifide.gradle.aem.common.utils.Patterns
import com.cognifide.gradle.aem.environment.Environment
import com.cognifide.gradle.aem.environment.EnvironmentPlugin
import com.cognifide.gradle.aem.instance.*
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.pkg.tasks.PackageCompose
import com.cognifide.gradle.aem.tooling.ToolingPlugin
import com.cognifide.gradle.aem.tooling.vlt.VltException
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File
import java.io.Serializable
import java.time.ZoneId
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Internal

/**
 * Core of library, facade for implementing tasks, configuration aggregator.
 */
@Suppress("TooManyFunctions")
class AemExtension(@JsonIgnore val project: Project) : Serializable {

    @JsonIgnore
    val logger = project.logger

    /**
     * Allows to read project property specified in command line and system property as a fallback.
     */
    @JsonIgnore
    val props = PropertyParser(this)

    /**
     * Project under which common configuration files are stored.
     * Usually it is also a project which is building full assembly CRX package.
     *
     * Convention assumes in case of:
     * - multi-project build - subproject with path ':aem'
     * - single-project build - root project
     */
    @JsonIgnore
    val projectMain: Project = project.findProject(props.string("projectMainPath") ?: ":aem") ?: project.rootProject

    /**
     * Project name convention prefixes used to determine default:
     *
     * - bundle install subdirectory
     * - CRX package base name
     * - OSGi bundle JAR base name
     *
     * in case of multi-project build and assembly packages.
     */
    val projectPrefixes: List<String> = props.list("projectPrefixes") ?: listOf("aem.", "aem-", "aem_")

    /**
     * Project name with skipped convention prefixes.
     */
    val projectName: String
        get() = project.name.run {
            var n = this; projectPrefixes.forEach { n = n.removePrefix(it) }; n
        }

    /**
     * Base name used as default for CRX packages being created by compose or collect task
     * and also for OSGi bundle JARs.
     */
    val baseName: String
        get() = Formats.normalizeSeparators(if (project == project.rootProject) {
            project.rootProject.name
        } else {
            "${project.rootProject.name}.$projectName"
        }, ".")

    /**
     * Determines current environment name to be used in e.g package deployment.
     */
    val env: String = props.string("env") ?: run { System.getenv("ENV") ?: "local" }

    /**
     * Timezone ID (default for defined instances)
     */
    @JsonIgnore
    val zoneId: ZoneId = props.string("zoneId")?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()

    /**
     * Specify characters to be used as line endings when cleaning up checked out JCR content.
     */
    var lineSeparator: String = props.string("lineSeparator") ?: "LF"

    @JsonIgnore
    val lineSeparatorString: String = LineSeparator.string(lineSeparator)

    /**
     * Directory for storing project specific files used by plugin e.g:
     * - Groovy Scripts to be launched by Groovy Console instance service in tasks defined in project.
     */
    val configDir: File
        get() = project.file(props.string("configDir") ?: "gradle")

    /**
     * Directory for storing common files used by plugin e.g:
     * - CRX package thumbnail
     * - environment configuration files (HTTPD virtual hosts, Dispatcher)
     * - instance overrides files
     * - tail incident filter
     */
    val configCommonDir: File
        get() = projectMain.file(props.string("configCommonDir") ?: "gradle")

    /**
     * Performs parallel CRX package deployments and instance synchronization.
     */
    @JsonIgnore
    val parallel = ParallelExecutor(this)

    /**
     * TODO to be merged with upcoming file transfer impl
     */
    val resolverOptions = ResolverOptions(this)

    /**
     * Multi-protocol file transfer facade that allows to list, upload and download files available at remote servers.
     */
    @get:Internal
    val fileTransfer: FileTransfer = FileMultiTransfer(this)

    /**
     * Customize file resolver options like default credentials, remote host checking etc.
     */
    fun resolver(options: ResolverOptions.() -> Unit) = resolverOptions.apply(options)

    val packageOptions = PackageOptions(this)

    /**
     * Defines common settings for built packages and deployment related behavior.
     */
    fun `package`(options: PackageOptions.() -> Unit) {
        packageOptions.apply(options)
    }

    fun pkg(options: PackageOptions.() -> Unit) = `package`(options)

    val instanceOptions = InstanceOptions(this)

    /**
     * Defines instances to work with.
     */
    fun instance(options: InstanceOptions.() -> Unit) {
        instanceOptions.apply(options)
    }

    val localInstanceManager = LocalInstanceManager(this)

    /**
     * Define common settings valid only for instances created at local file system.
     */
    fun localInstance(options: LocalInstanceManager.() -> Unit) = localInstanceManager.apply(options)

    /**
     * Provides API for controlling virtualized AEM environment with HTTPD and dispatcher module.
     */
    val environment = Environment(this)

    fun environment(configurer: Environment.() -> Unit) {
        environment.apply(configurer)
    }

    /**
     * Provides API for displaying interactive notification during running build tasks.
     */
    val notifier = NotifierFacade.of(this)

    fun notifier(configurer: NotifierFacade.() -> Unit) {
        notifier.apply(configurer)
    }

    /**
     * Provides API for easier creation of tasks (e.g in sequence) in the matter of Gradle task configuration avoidance.
     */
    val tasks = AemTaskFacade(this)

    fun tasks(configurer: AemTaskFacade.() -> Unit) {
        tasks.apply(configurer)
    }

    /**
     * Provides API for performing actions affecting multiple instances at once.
     */
    @JsonIgnore
    val instanceActions = InstanceActionPerformer(this)

    /**
     * Collection of all java packages from all projects applying bundle plugin.
     */
    val javaPackages: List<String>
        get() = AemPlugin.withId(project, BundlePlugin.ID).flatMap { subproject ->
            of(subproject).tasks.bundles.mapNotNull { it.javaPackage }
        }

    @get:JsonIgnore
    val instances: List<Instance>
        get() = filterInstances()

    fun instances(consumer: (Instance) -> Unit) = parallel.with(instances, consumer)

    fun instances(filter: String, consumer: (Instance) -> Unit) = parallel.with(filterInstances(filter), consumer)

    fun instance(urlOrName: String): Instance = instanceOptions.parse(urlOrName)

    fun instances(urlsOrNames: Collection<String>): List<Instance> = urlsOrNames.map { instance(it) }

    @get:JsonIgnore
    val anyInstance: Instance
        get() {
            val cmdInstanceArg = props.string("instance")
            if (!cmdInstanceArg.isNullOrBlank()) {
                return instance(cmdInstanceArg)
            }

            return namedInstance(Instance.FILTER_ANY)
        }

    fun namedInstance(desiredName: String? = props.string("instance.name"), defaultName: String = "$env-*"): Instance {
        val nameMatcher: String = desiredName ?: defaultName

        val namedInstance = filterInstances(nameMatcher).firstOrNull()
        if (namedInstance != null) {
            return namedInstance
        }

        throw InstanceException("Instance named '$nameMatcher' is not defined.")
    }

    fun filterInstances(nameMatcher: String = props.string("instance.name") ?: "$env-*"): List<Instance> {
        val all = instanceOptions.defined.values

        // Specified by command line should not be filtered
        val cmd = all.filter { it.environment == Instance.ENVIRONMENT_CMD }
        if (cmd.isNotEmpty()) {
            return cmd
        }

        // Defined by build script, via properties or defaults are filterable by name
        return all.filter { instance ->
            when {
                props.flag("instance.authors") -> {
                    Patterns.wildcard(instance.name, "$env-${InstanceType.AUTHOR}*")
                }
                props.flag("instance.publishers") -> {
                    Patterns.wildcard(instance.name, "$env-${InstanceType.PUBLISH}*")
                }
                else -> Patterns.wildcard(instance.name, nameMatcher)
            }
        }
    }

    @get:JsonIgnore
    val authorInstances: List<Instance>
        get() = filterInstances().filter { it.type == InstanceType.AUTHOR }

    fun authorInstances(consumer: (Instance) -> Unit) = parallel.with(authorInstances, consumer)

    @get:JsonIgnore
    val publishInstances: List<Instance>
        get() = filterInstances().filter { it.type == InstanceType.PUBLISH }

    fun publishInstances(consumer: Instance.() -> Unit) = parallel.with(publishInstances, consumer)

    @get:JsonIgnore
    val localInstances: List<LocalInstance>
        get() = instances.filterIsInstance(LocalInstance::class.java)

    fun localInstances(consumer: LocalInstance.() -> Unit) = parallel.with(localInstances, consumer)

    @get:JsonIgnore
    val remoteInstances: List<RemoteInstance>
        get() = instances.filterIsInstance(RemoteInstance::class.java)

    fun remoteInstances(consumer: RemoteInstance.() -> Unit) = parallel.with(remoteInstances, consumer)

    @get:JsonIgnore
    val packages: List<File>
        get() = project.tasks.withType(PackageCompose::class.java)
                .map { it.archiveFile.get().asFile }

    fun dependentPackages(task: Task): List<File> {
        return task.taskDependencies.getDependencies(task)
                .filterIsInstance(PackageCompose::class.java)
                .map { it.archiveFile.get().asFile }
    }

    fun sync(synchronizer: InstanceSync.() -> Unit) = sync(instances, synchronizer)

    fun <T> sync(instance: Instance, synchronizer: InstanceSync.() -> T) = instance.sync(synchronizer)

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

    fun <T> progress(action: ProgressIndicator.() -> T) = progressIndicator(action)

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

    @get:JsonIgnore
    val filter: VltFilter
        get() {
            val cmdFilterRoots = props.list("filter.roots") ?: listOf()
            if (cmdFilterRoots.isNotEmpty()) {
                logger.debug("Using Vault filter roots specified as command line property: $cmdFilterRoots")
                return VltFilter.temporary(project, cmdFilterRoots)
            }

            val cmdFilterPath = props.string("filter.path") ?: ""
            if (cmdFilterPath.isNotEmpty()) {
                val cmdFilter = FileOperations.find(project, packageOptions.vltRootDir.toString(), cmdFilterPath)
                        ?: throw VltException("Vault check out filter file does not exist at path: $cmdFilterPath" +
                                " (or under directory: ${packageOptions.vltRootDir}).")
                logger.debug("Using Vault filter file specified as command line property: $cmdFilterPath")
                return VltFilter(cmdFilter)
            }

            val conventionFilterFiles = listOf(
                    "${packageOptions.vltRootDir}/${VltFilter.CHECKOUT_NAME}",
                    "${packageOptions.vltRootDir}/${VltFilter.BUILD_NAME}"
            )
            val conventionFilterFile = FileOperations.find(project, packageOptions.vltRootDir.toString(), conventionFilterFiles)
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

    @get:JsonIgnore
    val temporaryDir: File
        get() = temporaryDir(TEMPORARY_DIR)

    fun fileWatcher(options: FileWatcher.() -> Unit) {
        FileWatcher(this).apply(options).start()
    }

    companion object {

        const val NAME = "aem"

        const val TEMPORARY_DIR = "tmp"

        private val PLUGIN_IDS = listOf(
                PackagePlugin.ID,
                BundlePlugin.ID,
                InstancePlugin.ID,
                EnvironmentPlugin.ID,
                ToolingPlugin.ID,
                CommonPlugin.ID
        )

        fun of(project: Project): AemExtension {
            return project.extensions.findByType(AemExtension::class.java)
                    ?: throw AemException("${project.displayName.capitalize()} must have at least one of following plugins applied: $PLUGIN_IDS")
        }
    }
}