package com.cognifide.gradle.aem.common

import com.cognifide.gradle.aem.bundle.BundleJar
import com.cognifide.gradle.aem.instance.tasks.*
import com.cognifide.gradle.aem.pkg.tasks.*
import com.cognifide.gradle.aem.tooling.tasks.Debug
import com.cognifide.gradle.aem.tooling.tasks.Rcp
import com.cognifide.gradle.aem.tooling.tasks.Sync
import com.cognifide.gradle.aem.tooling.tasks.Vlt
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test

@Suppress("TooManyFunctions")
class TaskFacade(private val aem: AemExtension) {

    private val project = aem.project

    private val bundleMap = mutableMapOf<String, BundleJar>()

    val bundles: List<BundleJar> = getAll(Jar::class.java).map { bundle(it) }

    internal fun jarsAsBundles() {
        initializeJarsAsBundles()
        project.afterEvaluate {
            finalizeJarsAsBundles()
        }
    }

    // lazy task configuration shorthands

    fun bundle(configurer: BundleJar.() -> Unit) = bundle(JavaPlugin.JAR_TASK_NAME, configurer)

    fun bundle(jarTaskName: String, configurer: BundleJar.() -> Unit) {
        named(jarTaskName, Jar::class.java) { bundle(this, configurer) }
    }

    fun await(configurer: Await.() -> Unit) = named(Await.NAME, Await::class.java, configurer)

    fun collect(configurer: Collect.() -> Unit) = named(Collect.NAME, Collect::class.java, configurer)

    fun create(configurer: Create.() -> Unit) = named(Create.NAME, Create::class.java, configurer)

    fun backup(configurer: Backup.() -> Unit) = named(Backup.NAME, Backup::class.java, configurer)

    fun destroy(configurer: Destroy.() -> Unit) = named(Destroy.NAME, Destroy::class.java, configurer)

    fun down(configurer: Down.() -> Unit) = named(Down.NAME, Down::class.java, configurer)

    fun reload(configurer: Reload.() -> Unit) = named(Reload.NAME, Reload::class.java, configurer)

    fun resetup(configurer: Resetup.() -> Unit) = named(Resetup.NAME, Resetup::class.java, configurer)

    fun resolve(configurer: Resolve.() -> Unit) = named(Resolve.NAME, Resolve::class.java, configurer)

    fun restart(configurer: Restart.() -> Unit) = named(Restart.NAME, Restart::class.java, configurer)

    fun satisfy(configurer: Satisfy.() -> Unit) = named(Satisfy.NAME, Satisfy::class.java, configurer)

    fun setup(configurer: Setup.() -> Unit) = named(Setup.NAME, Setup::class.java, configurer)

    fun up(configurer: Up.() -> Unit) = named(Up.NAME, Up::class.java, configurer)

    fun activate(configurer: Activate.() -> Unit) = named(Activate.NAME, Activate::class.java, configurer)

    fun compose(configurer: Compose.() -> Unit) = named(Compose.NAME, Compose::class.java, configurer)

    fun delete(configurer: Delete.() -> Unit) = named(Delete.NAME, Delete::class.java, configurer)

    fun deploy(configurer: Deploy.() -> Unit) = named(Deploy.NAME, Deploy::class.java, configurer)

    fun install(configurer: Install.() -> Unit) = named(Install.NAME, Install::class.java, configurer)

    fun purge(configurer: Purge.() -> Unit) = named(Purge.NAME, Purge::class.java, configurer)

    fun uninstall(configurer: Uninstall.() -> Unit) = named(Uninstall.NAME, Uninstall::class.java, configurer)

    fun upload(configurer: Upload.() -> Unit) = named(Upload.NAME, Upload::class.java, configurer)

    fun debug(configurer: Debug.() -> Unit) = named(Debug.NAME, Debug::class.java, configurer)

    fun rcp(configurer: Rcp.() -> Unit) = named(Rcp.NAME, Rcp::class.java, configurer)

    fun sync(configurer: Sync.() -> Unit) = named(Sync.NAME, Sync::class.java, configurer)

    fun vlt(configurer: Vlt.() -> Unit) = named(Vlt.NAME, Vlt::class.java, configurer)

    // generic API & internals

    internal fun bundle(jarTaskPath: String): BundleJar {
        return bundle(get(jarTaskPath, Jar::class.java))
    }

    internal fun bundle(jar: Jar, configurer: BundleJar.() -> Unit = {}): BundleJar {
        return bundleMap.getOrPut(jar.name) { BundleJar(aem, jar) }.apply(configurer)
    }

    fun pathed(path: String): TaskProvider<Task> {
        val projectPath = path.substringBeforeLast(":", project.path).ifEmpty { ":" }
        val taskName = path.substringAfterLast(":")
        val subproject = project.project(projectPath)

        return try {
            subproject.tasks.named(taskName)
        } catch (e: UnknownTaskException) {
            throw composeException(taskName, project = subproject)
        }
    }

    fun pathed(paths: Collection<Any>): List<TaskProvider<out Task>> {
        return paths.map { path ->
            when (path) {
                is String -> pathed(path)
                is TaskProvider<*> -> path
                else -> throw IllegalArgumentException("Illegal task argument: $path")
            }
        }
    }

    @Suppress("unchecked_cast")
    fun <T : Task> named(name: String): TaskProvider<T> {
        return try {
            project.tasks.named(name) as TaskProvider<T>
        } catch (e: UnknownTaskException) {
            throw composeException(name)
        }
    }

    fun <T : Task> named(name: String, type: Class<T>, configurer: T.() -> Unit) {
        try {
            project.tasks.named(name, type, configurer)
        } catch (e: UnknownTaskException) {
            throw composeException(name)
        }
    }

    fun <T : Task> typed(type: Class<T>, configurer: T.() -> Unit) {
        project.tasks.withType(type).configureEach(configurer)
    }

    inline fun <reified T : Task> register(name: String, noinline configurer: T.() -> Unit = {}): TaskProvider<T> {
        return register(name, T::class.java, configurer)
    }

    fun <T : Task> register(name: String, clazz: Class<T>, configurer: T.() -> Unit = {}): TaskProvider<T> {
        with(project) {
            val provider = tasks.register(name, clazz) { task ->
                task.group = AemTask.GROUP
                task.apply(configurer)
            }

            afterEvaluate { provider.configure { if (it is AemTask) it.projectEvaluated() } }
            gradle.projectsEvaluated { provider.configure { if (it is AemTask) it.projectsEvaluated() } }
            gradle.taskGraph.whenReady { graph -> provider.configure { if (it is AemTask) it.taskGraphReady(graph) } }

            return provider
        }
    }

    fun register(name: String, configurer: AemDefaultTask.() -> Unit) {
        register(name, AemDefaultTask::class.java, configurer)
    }

    @Suppress("unchecked_cast")
    fun <T : Task> get(path: String, type: Class<T>): T {
        val task = if (path.contains(":")) {
            project.tasks.findByPath(path)
        } else {
            project.tasks.findByName(path)
        }

        if (task == null || !type.isInstance(task)) {
            throw composeException(path, type)
        }

        return task as T
    }

    fun <T : Task> getAll(type: Class<T>) = project.tasks.withType(type).toList()

    fun sequence(name: String, sequenceOptions: SequenceOptions.() -> Unit) = sequence(name, sequenceOptions) {}

    fun sequence(name: String, sequenceOptions: SequenceOptions.() -> Unit, taskOptions: Task.() -> Unit): TaskProvider<Task> {
        val sequence = project.tasks.register(name)

        project.gradle.projectsEvaluated { _ ->
            val options = SequenceOptions().apply(sequenceOptions)
            val taskList = pathed(options.dependentTasks)
            val afterList = pathed(options.afterTasks)

            if (taskList.size > 1) {
                for (i in 1 until taskList.size) {
                    val previous = taskList[i - 1]
                    val current = taskList[i]

                    current.configure { it.mustRunAfter(previous) }
                }
            }
            taskList.forEach { task ->
                task.configure { it.mustRunAfter(afterList) }
            }

            sequence.configure { task ->
                task.group = AemTask.GROUP
                task.dependsOn(taskList).mustRunAfter(afterList)
                task.apply(taskOptions)
            }
        }

        return sequence
    }

    private fun initializeJarsAsBundles() {
        project.tasks.withType(Jar::class.java) {
            bundle { initialize() }
        }
    }

    private fun finalizeJarsAsBundles() {
        project.tasks.withType(Jar::class.java) {
            bundle { finalize() }
        }

        // @see <https://github.com/Cognifide/gradle-aem-plugin/issues/95>
        named(JavaPlugin.TEST_TASK_NAME, Test::class.java) {
            val testImplConfig = project.configurations.getByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME)
            val compileOnlyConfig = project.configurations.getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME)

            testImplConfig.extendsFrom(compileOnlyConfig)

            project.tasks.withType(Jar::class.java).forEach { jar ->
                dependsOn(jar)
                classpath += project.files(jar.archivePath)
            }
        }
    }

    private fun composeException(taskName: String, type: Class<*>? = null, cause: Exception? = null, project: Project = this.project): AemException {
        val msg = if (type != null) {
            "${project.displayName.capitalize()} does not have task '$taskName' of type '$type'. Ensure correct plugins applied."
        } else {
            "${project.displayName.capitalize()} does not have task '$taskName'. Ensure correct plugins applied."
        }

        return if (cause != null) {
            AemException(msg, cause)
        } else {
            AemException(msg)
        }
    }

    class SequenceOptions {

        var dependentTasks: Collection<Any> = listOf()

        var afterTasks: Collection<Any> = listOf()

        fun dependsOn(vararg tasks: Any) {
            dependsOn(tasks.toList())
        }

        fun dependsOn(tasks: Collection<Any>) {
            dependentTasks = tasks
        }

        fun mustRunAfter(vararg tasks: Any) {
            mustRunAfter(tasks.toList())
        }

        fun mustRunAfter(tasks: Collection<Any>) {
            afterTasks = tasks
        }
    }
}