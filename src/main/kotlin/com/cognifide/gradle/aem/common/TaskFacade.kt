package com.cognifide.gradle.aem.common

import com.cognifide.gradle.aem.bundle.BundleJar
import com.cognifide.gradle.aem.bundle.BundlePlugin
import com.cognifide.gradle.aem.instance.tasks.Await
import com.cognifide.gradle.aem.instance.tasks.Satisfy
import com.cognifide.gradle.aem.instance.tasks.Setup
import com.cognifide.gradle.aem.pkg.tasks.Compose
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar

class TaskFacade(private val aem: AemExtension) {

    private val project = aem.project

    private val bundleMap = mutableMapOf<String, BundleJar>()

    init {
        project.gradle.projectsEvaluated { _ ->
            if (project.plugins.hasPlugin(BundlePlugin.ID)) {
                bundle(JavaPlugin.JAR_TASK_NAME) // forces default jar to be configured
            }
            bundles.values.forEach { it.projectsEvaluated() }
        }
    }

    fun compose(configurer: Compose.() -> Unit) = named(Compose.NAME, Compose::class.java, configurer)

    fun bundle(configurer: BundleJar.() -> Unit) = bundle(JavaPlugin.JAR_TASK_NAME, configurer)

    fun bundle(jarTaskName: String, configurer: BundleJar.() -> Unit) {
        named(jarTaskName, Jar::class.java) { bundle(this, configurer) }
    }

    internal fun bundle(jarTaskPath: String): BundleJar {
        return bundle(get(jarTaskPath, Jar::class.java))
    }

    internal fun bundle(jar: Jar, configurer: BundleJar.() -> Unit = {}): BundleJar {
        return bundleMap.getOrPut(jar.name) { BundleJar(aem, jar) }.apply(configurer)
    }

    /**
     * Contains OSGi bundle configuration used in case of composing CRX package.
     */
    @Nested
    val bundles: Map<String, BundleJar> = bundleMap

    fun satisfy(configurer: Satisfy.() -> Unit) = named(Satisfy.NAME, Satisfy::class.java, configurer)

    fun setup(configurer: Setup.() -> Unit) = named(Setup.NAME, Setup::class.java, configurer)

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

    fun <T : Task> copy(name: String, suffix: String, type: Class<T>, configurer: T.() -> Unit = {}): TaskProvider<T> {
        return project.tasks.register("$name${suffix.capitalize()}", type) { task ->
            task.group = AemTask.GROUP
            task.apply(configurer)
        }
    }

    fun await(suffix: String, configurer: Await.() -> Unit = {}) = copy(Await.NAME, suffix, Await::class.java, configurer)

    fun <T : Task> register(name: String, clazz: Class<T>): TaskProvider<T> {
        return register(name, clazz) {}
    }

    fun <T : Task> register(name: String, clazz: Class<T>, configurer: T.() -> Unit): TaskProvider<T> {
        with(project) {
            val provider = tasks.register(name, clazz, configurer)

            afterEvaluate { provider.configure { if (it is AemTask) it.projectEvaluated() } }
            gradle.projectsEvaluated { provider.configure { if (it is AemTask) it.projectsEvaluated() } }
            gradle.taskGraph.whenReady { graph -> provider.configure { if (it is AemTask) it.taskGraphReady(graph) } }

            return provider
        }
    }

    @Suppress("unchecked_cast")
    fun <T : Task> get(path: String, type: Class<T>): T {
        val task = if (path.contains(":")) {
            project.tasks.getByPath(path)
        } else {
            project.tasks.findByName(path)
        }

        if (task == null || !type.isInstance(task)) {
            throw composeException(path, type)
        }

        return task as T
    }

    fun <T : Task> getAll(type: Class<T>) = project.tasks.withType(type).toList()

    fun sequence(name: String, configurer: SequenceOptions.() -> Unit): TaskProvider<Task> {
        val sequence = project.tasks.register(name)

        project.gradle.projectsEvaluated { _ ->
            val options = SequenceOptions().apply(configurer)
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
            }
        }

        return sequence
    }

    private fun composeException(taskName: String, type: Class<*>? = null, cause: Exception? = null, project: Project = this.project): AemException {
        val msg = if (type != null) {
            "Project '${project.displayName}' does not have task '$taskName' of type '$type'. Ensure correct plugins applied."
        } else {
            "Project '${project.displayName}' does not have task '$taskName'. Ensure correct plugins applied."
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