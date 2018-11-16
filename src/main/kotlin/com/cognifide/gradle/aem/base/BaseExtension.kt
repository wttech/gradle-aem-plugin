package com.cognifide.gradle.aem.base

import com.cognifide.gradle.aem.api.AemException
import com.cognifide.gradle.aem.base.vlt.VltFilter
import com.cognifide.gradle.aem.bundle.BundleExtension
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.InstancePlugin
import com.cognifide.gradle.aem.instance.InstanceSync
import com.cognifide.gradle.aem.instance.LocalHandle
import com.cognifide.gradle.aem.internal.Formats
import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.internal.http.HttpClient
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.pkg.tasks.Compose
import com.fasterxml.jackson.annotation.JsonIgnore
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.bundling.Jar
import java.io.File

/**
 * Main place for providing build script DSL capabilities in case of AEM.
 */
open class BaseExtension(@Internal val project: Project
) {

    @Internal
    val logger = project.logger

    /**
     * Allows to read project property specified in command line and system property as a fallback.
     */
    @Internal
    val props = PropertyParser(this, project)

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

    @Input
    val bundles = mutableMapOf<String, BundleExtension>()

    @Internal
    val notifier = Notifier.of(this)

    @Internal
    val tasks = TaskFactory(project)

    @get:Internal
    val instances: List<Instance>
        get() = Instance.filter(project)

    fun instances(filter: String): List<Instance> {
        return Instance.filter(project, filter)
    }

    fun instances(consumer: (Instance) -> Unit) {
        instances.parallelStream().forEach(consumer)
    }

    fun instances(filter: String, consumer: (Instance) -> Unit) {
        instances(filter).parallelStream().forEach(consumer)
    }

    fun instance(urlOrName: String): Instance {
        return config.parseInstance(urlOrName)
    }

    fun instanceAny() = Instance.any(project)

    fun instanceConcrete(type: String) = Instance.concrete(project, type)

    @get:Internal
    val handles: List<LocalHandle>
        get() = Instance.handles(project)

    fun handles(consumer: LocalHandle.() -> Unit) {
        handles(handles, consumer)
    }

    fun handles(handles: Collection<LocalHandle>, consumer: LocalHandle.() -> Unit) {
        handles.parallelStream().forEach(consumer)
    }

    fun packages(consumer: (File) -> Unit) {
        project.tasks.withType(Compose::class.java)
                .map { it.archivePath }
                .parallelStream()
                .forEach(consumer)
    }

    @get:Internal
    val packages: List<File>
        get() = project.tasks.withType(Compose::class.java)
                .map { it.archivePath }

    fun packages(task: Task): List<File> {
        return task.taskDependencies.getDependencies(task)
                .filterIsInstance(Compose::class.java)
                .map { it.archivePath }
    }

    fun sync(synchronizer: InstanceSync.() -> Unit) = sync(instances, synchronizer)

    fun sync(instances: Collection<Instance>, synchronizer: InstanceSync.() -> Unit) {
        instances.parallelStream().forEach { it.sync(synchronizer) }
    }

    fun syncPackages(synchronizer: InstanceSync.(File) -> Unit) = syncPackages(instances, packages, synchronizer)

    fun syncPackages(instances: Collection<Instance>, packages: Collection<File>, synchronizer: InstanceSync.(File) -> Unit) {
        packages.forEach { p -> // single AEM instance dislikes parallel package installation
            instances.parallelStream().forEach { i ->
                InstanceSync(project, i).apply { synchronizer(p) }
            }
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

    fun bundle(configurer: BundleExtension.() -> Unit) {
        bundle(JavaPlugin.JAR_TASK_NAME, configurer)
    }

    fun bundle(jarTaskName: String, configurer: BundleExtension.() -> Unit) {
        project.tasks.withType(Jar::class.java)
                .named(jarTaskName)
                .configure { bundle(it, configurer) }
    }

    @get:Internal
    val bundle: BundleExtension
        get() = bundle(JavaPlugin.JAR_TASK_NAME)

    fun bundle(jarTaskName: String) = bundle(project.tasks.getByName(jarTaskName) as Jar)

    fun bundle(jar: Jar, configurer: BundleExtension.() -> Unit = {}): BundleExtension {
        return bundles.getOrPut(jar.name) { BundleExtension(this, jar).apply(configurer) }
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

    fun filter(): VltFilter = VltFilter.determine(project)

    fun filter(file: File) = VltFilter(file)

    fun filter(path: String) = filter(project.file(path))

    init {
        project.gradle.projectsEvaluated {
            bundles.values.forEach { it.projectsEvaluated() }
        }
    }

    companion object {

        const val NAME = "aem"

        /**
         * Token indicating that value need to be corrected later by more advanced logic / convention.
         */
        const val AUTO_DETERMINED = "<auto>"

        fun of(project: Project): BaseExtension {
            return project.extensions.findByType(BaseExtension::class.java)
                    ?: throw AemException("${project.displayName.capitalize()} has neither '${PackagePlugin.ID}' nor '${InstancePlugin.ID}' plugin applied.")
        }

    }

}