package com.cognifide.gradle.aem.common

import com.cognifide.gradle.aem.bundle.BundlePlugin
import com.cognifide.gradle.aem.bundle.tasks.Bundle
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.http.HttpClient
import com.cognifide.gradle.aem.config.Config
import com.cognifide.gradle.aem.config.ConfigPlugin
import com.cognifide.gradle.aem.instance.*
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.pkg.tasks.Compose
import com.cognifide.gradle.aem.tooling.*
import com.cognifide.gradle.aem.tooling.vlt.VltException
import com.cognifide.gradle.aem.tooling.vlt.VltFilter
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
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
    val projectPrefixes: List<String> = listOf("aem.", "aem-", "aem_")

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
     * Toggles parallel CRX package deployments and instance synchronization.
     */
    @Internal
    val parallel = props.boolean("aem.parallel") ?: true

    /**
     * Collection of common AEM configuration properties like instance definitions. Contains default values for tasks.
     */
    @Nested
    val config = Config(this)

    /**
     * Provides API for displaying interactive notification during running build tasks.
     */
    @Internal
    val notifier = NotifierFacade.of(this)

    /**
     * Provides API for easier creation of tasks (e.g in sequence) in the matter of Gradle task configuration avoidance.
     */
    @Internal
    val tasks = TaskFactory(project)

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
        get() = project.allprojects.filter {
            it.plugins.hasPlugin(BundlePlugin.ID)
        }.flatMap { subproject ->
            AemExtension.of(subproject).bundles.mapNotNull { it.javaPackage }
        }

    @get:Internal
    val instances: List<Instance>
        get() = filterInstances()

    fun instances(consumer: (Instance) -> Unit) = parallelWith(instances, consumer)

    fun instances(filter: String, consumer: (Instance) -> Unit) = parallelWith(filterInstances(filter), consumer)

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

    fun authorInstances(consumer: (Instance) -> Unit) = parallelWith(authorInstances, consumer)

    @get:Internal
    val publishInstances: List<Instance>
        get() = filterInstances().filter { it.type == InstanceType.PUBLISH }

    fun publishInstances(consumer: Instance.() -> Unit) = parallelWith(publishInstances, consumer)

    @get:Internal
    val localInstances: List<LocalInstance>
        get() = instances.filterIsInstance(LocalInstance::class.java)

    fun localInstances(consumer: LocalInstance.() -> Unit) = parallelWith(localInstances, consumer)

    @get:Internal
    val localHandles: List<LocalHandle>
        get() = localInstances.map { LocalHandle(project, it) }

    fun localHandles(consumer: LocalHandle.() -> Unit) = parallelWith(localHandles, consumer)

    @get:Internal
    val remoteInstances: List<RemoteInstance>
        get() = instances.filterIsInstance(RemoteInstance::class.java)

    fun remoteInstances(consumer: RemoteInstance.() -> Unit) = parallelWith(remoteInstances, consumer)

    fun packages(consumer: (File) -> Unit) = parallelWith(packages, consumer)

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
        parallelWith(instances) { this.sync.apply(synchronizer) }
    }

    fun syncPackages(synchronizer: InstanceSync.(File) -> Unit) = syncPackages(instances, packages, synchronizer)

    fun syncPackages(
        instances: Collection<Instance>,
        packages: Collection<File>,
        synchronizer: InstanceSync.(File) -> Unit
    ) {
        packages.forEach { pkg -> // single AEM instance dislikes parallel package installation
            parallelWith(instances) { // but same package could be in parallel deployed on different AEM instances
                sync.apply { synchronizer(pkg) }
            }
        }
    }

    fun <T> http(consumer: HttpClient.() -> T) = HttpClient(project).run(consumer)

    fun config(configurer: Config.() -> Unit) {
        config.apply(configurer)
    }

    @get:Internal
    val compose: Compose
        get() = compose(Compose.NAME)

    fun compose(configurer: Compose.() -> Unit) {
        project.tasks.withType(Compose::class.java).named(Compose.NAME).configure(configurer)
    }

    fun compose(taskName: String) = project.tasks.getByName(taskName) as Compose

    @get:Internal
    val composes: List<Compose> = project.tasks.withType(Compose::class.java).toList()

    @get:Internal
    val bundle: Bundle
        get() = bundle(Bundle.NAME)

    fun bundle(configurer: Bundle.() -> Unit) {
        project.tasks.withType(Bundle::class.java).named(Bundle.NAME).configure(configurer)
    }

    fun bundle(taskName: String) = project.tasks.getByName(taskName) as Bundle

    @get:Internal
    val bundles: List<Bundle> = project.tasks.withType(Bundle::class.java).toList()

    fun notifier(configurer: NotifierFacade.() -> Unit) {
        notifier.apply(configurer)
    }

    fun tasks(configurer: TaskFactory.() -> Unit) {
        tasks.apply(configurer)
    }

    fun retry(configurer: Retry.() -> Unit): Retry {
        return retry().apply(configurer)
    }

    fun retry(): Retry = Retry.none()

    fun <T> progress(options: ProgressIndicator.() -> Unit, action: ProgressIndicator.() -> T): T {
        return ProgressIndicator(project).apply(options).launch(action)
    }

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

    fun <A, B : Any> parallelMap(iterable: Iterable<A>, mapper: (A) -> B): Collection<B> {
        return parallelMap(iterable, { true }, mapper)
    }

    fun <A, B : Any> parallelMap(iterable: Iterable<A>, filter: (A) -> Boolean, mapper: (A) -> B): List<B> {
        if (!parallel) {
            return iterable.filter(filter).map(mapper)
        }

        return runBlocking(Dispatchers.Default) {
            iterable.map { value -> async { value.takeIf(filter)?.let(mapper) } }.mapNotNull { it.await() }
        }
    }

    fun <A> parallelWith(iterable: Iterable<A>, callback: A.() -> Unit) {
        if (!parallel) {
            return iterable.forEach { it.apply(callback) }
        }

        return runBlocking(Dispatchers.Default) {
            iterable.map { value -> async { value.apply(callback) } }.forEach { it.await() }
        }
    }

    fun temporaryDir(task: Task) = temporaryDir(task.name)

    fun temporaryDir(name: String) = AemTask.temporaryDir(project, name)

    companion object {

        const val NAME = "aem"

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