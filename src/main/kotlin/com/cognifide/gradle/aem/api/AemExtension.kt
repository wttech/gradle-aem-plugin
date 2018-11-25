package com.cognifide.gradle.aem.api

import com.cognifide.gradle.aem.base.BaseConfig
import com.cognifide.gradle.aem.base.Notifier
import com.cognifide.gradle.aem.base.Retry
import com.cognifide.gradle.aem.base.TaskFactory
import com.cognifide.gradle.aem.base.vlt.VltException
import com.cognifide.gradle.aem.base.vlt.VltFilter
import com.cognifide.gradle.aem.bundle.BundleJar
import com.cognifide.gradle.aem.bundle.BundlePlugin
import com.cognifide.gradle.aem.instance.*
import com.cognifide.gradle.aem.internal.Formats
import com.cognifide.gradle.aem.internal.Patterns
import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.internal.file.FileOperations
import com.cognifide.gradle.aem.internal.http.HttpClient
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.pkg.tasks.Compose
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File
import java.util.concurrent.ForkJoinPool
import java.util.stream.Collectors
import java.util.stream.StreamSupport
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.bundling.Jar

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
    val environment: String = props.string("aem.env") {
        System.getenv("AEM_ENV") ?: "local"
    }

    @Nested
    val config = BaseConfig(this)

    @Nested
    val bundles = mutableMapOf<String, BundleJar>()

    @Internal
    val notifier = Notifier.of(this)

    @Internal
    val tasks = TaskFactory(project)

    @get:Internal
    val instances: List<Instance>
        get() = instanceNamed(props.string("aem.instance.name", "$environment-*"))

    fun instances(consumer: (Instance) -> Unit) = parallelWith(instances, consumer)

    fun instances(filter: String, consumer: (Instance) -> Unit) = parallelWith(instanceNamed(filter), consumer)

    fun instance(urlOrName: String): Instance {
        return config.parseInstance(urlOrName)
    }

    @get:Internal
    val instanceAny: Instance
        get() {
            val cmdInstanceArg = props.string("aem.instance")
            if (!cmdInstanceArg.isNullOrBlank()) {
                val cmdInstance = config.parseInstance(cmdInstanceArg)

                logger.info("Using instance specified by command line parameter: $cmdInstance")
                return cmdInstance
            }

            val anyInstance = instanceNamed(Instance.FILTER_ANY).firstOrNull()
            if (anyInstance != null) {
                logger.info("Using first instance matching filter '${Instance.FILTER_ANY}': $anyInstance")
                return anyInstance
            }

            throw InstanceException("Instance cannot be determined neither by command line parameter nor AEM config.")
        }

    fun instanceTyped(type: String): Instance? {
        return props.prop("aem.instance.$type")?.run { config.parseInstance(this) }
    }

    fun instanceNamed(nameMatcher: String): List<Instance> {
        val all = config.instances.values

        // Specified by command line should not be filtered
        val cmd = all.filter { it.environment == Instance.ENVIRONMENT_CMD }
        if (cmd.isNotEmpty()) {
            return cmd
        }

        // Defined by build script, via properties or defaults are filterable by name
        return all.filter { instance ->
            when {
                props.flag(Instance.AUTHORS_PROP) -> {
                    Patterns.wildcard(instance.name, "$environment-${InstanceType.AUTHOR}*")
                }
                props.flag(Instance.PUBLISHERS_PROP) -> {
                    Patterns.wildcard(instance.name, "$environment-${InstanceType.PUBLISH}*")
                }
                else -> Patterns.wildcard(instance.name, nameMatcher)
            }
        }
    }

    @get:Internal
    val instanceLocals: List<LocalInstance>
        get() = instances.filterIsInstance(LocalInstance::class.java)

    @get:Internal
    val instanceHandles: List<LocalHandle>
        get() = instanceLocals.map { LocalHandle(project, it) }

    @get:Internal
    val instanceRemotes: List<RemoteInstance>
        get() = instances.filterIsInstance(RemoteInstance::class.java)

    @get:Internal
    val instanceAuthors: List<Instance>
        get() = instanceNamed("$environment-${InstanceType.AUTHOR.type}*")

    @get:Internal
    val instancePublishers: List<Instance>
        get() = instanceNamed("$environment-${InstanceType.PUBLISH.type}*")

    fun instanceHandles(consumer: LocalHandle.() -> Unit) = parallelWith(instanceHandles, consumer)

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
        // single AEM instance dislikes parallel package installation
        packages.forEach { p ->
            // put same package could be in parallel deployed on different AEM instances
            parallelWith(instances) { sync.apply { synchronizer(p) } }
        }
    }

    fun <T> http(consumer: HttpClient.() -> T): T {
        return HttpClient(project).run(consumer)
    }

    fun http(consumer: HttpClient.() -> Unit) {
        HttpClient(project).run(consumer)
    }

    fun config(configurer: BaseConfig.() -> Unit) {
        config.apply(configurer)
    }

    @get:Internal
    val compose: Compose
        get() = compose(Compose.NAME)

    fun compose(taskName: String): Compose {
        return project.tasks.getByName(taskName) as Compose
    }

    @get:Internal
    val composes: List<Compose>
        get() = project.tasks.withType(Compose::class.java).toList()

    fun bundle(configurer: BundleJar.() -> Unit) {
        bundle(JavaPlugin.JAR_TASK_NAME, configurer)
    }

    fun bundle(jarTaskName: String, configurer: BundleJar.() -> Unit) {
        project.tasks.withType(Jar::class.java)
                .named(jarTaskName)
                .configure { bundle(it, configurer) }
    }

    @get:Internal
    val bundle: BundleJar
        get() = bundle(JavaPlugin.JAR_TASK_NAME)

    fun bundle(jarTaskName: String) = bundle(project.tasks.getByName(jarTaskName) as Jar)

    fun bundle(jar: Jar, configurer: BundleJar.() -> Unit = {}): BundleJar {
        return bundles.getOrPut(jar.name) { BundleJar(this, jar).apply(configurer) }
    }

    fun notifier(configurer: Notifier.() -> Unit) {
        notifier.apply(configurer)
    }

    fun tasks(configurer: TaskFactory.() -> Unit) {
        tasks.apply(configurer)
    }

    fun retry(configurer: Retry.() -> Unit): Retry {
        return retry().apply(configurer)
    }

    fun retry(): Retry = Retry.once()

    @get:Internal
    val filter: VltFilter
        get() {
            val cmdFilterRoots = props.list("aem.filter.roots")
            if (cmdFilterRoots.isNotEmpty()) {
                logger.debug("Using Vault filter roots specified as command line property: $cmdFilterRoots")
                return VltFilter.temporary(project, cmdFilterRoots)
            }

            val cmdFilterPath = props.string("aem.filter.path", "")
            if (cmdFilterPath.isNotEmpty()) {
                val cmdFilter = FileOperations.find(project, config.packageVltRoot, cmdFilterPath)
                        ?: throw VltException("Vault check out filter file does not exist at path: $cmdFilterPath (or under directory: ${config.packageVltRoot}).")
                logger.debug("Using Vault filter file specified as command line property: $cmdFilterPath")
                return VltFilter(cmdFilter)
            }

            val conventionFilterFiles = listOf("${config.packageVltRoot}/${VltFilter.CHECKOUT_NAME}", "${config.packageVltRoot}/${VltFilter.BUILD_NAME}")
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

    fun <A, B> parallelProcess(iterable: Iterable<A>, mapper: (A) -> B): Collection<B> {
        return parallelProcess(iterable, { true }, mapper)
    }

    @Internal
    var parallelPool = ForkJoinPool(PARALLEL_THREADS)

    fun <A, B> parallelProcess(iterable: Iterable<A>, filter: (A) -> Boolean, mapper: (A) -> B): List<B> {
        return parallelPool.submit<List<B>> {
            StreamSupport.stream(iterable.spliterator(), true)
                    .filter(filter)
                    .map(mapper)
                    .collect(Collectors.toList())
        }.get()
    }

    fun <A> parallelWith(iterable: Iterable<A>, callback: A.() -> Unit) {
        parallelPool.submit {
            StreamSupport.stream(iterable.spliterator(), true).forEach(callback)
        }.get()
    }

    init {
        project.gradle.projectsEvaluated { _ ->
            if (project.plugins.hasPlugin(BundlePlugin.ID)) {
                bundle // forces default jar to be configured
            }
            bundles.values.forEach { it.projectsEvaluated() }
        }
    }

    companion object {

        const val NAME = "aem"

        const val PARALLEL_THREADS = 8

        fun of(project: Project): AemExtension {
            return project.extensions.findByType(AemExtension::class.java)
                    ?: throw AemException("${project.displayName.capitalize()} has neither '${PackagePlugin.ID}' nor '${InstancePlugin.ID}' plugin applied.")
        }
    }
}