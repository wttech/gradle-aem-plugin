package com.cognifide.gradle.aem.common

import com.cognifide.gradle.aem.bundle.BundleJar
import com.cognifide.gradle.aem.bundle.BundlePlugin
import com.cognifide.gradle.aem.instance.tasks.Await
import com.cognifide.gradle.aem.pkg.tasks.Compose
import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar

class TaskFactory(private val aem: AemExtension) {

    private val project = aem.project

    private val bundleMap = mutableMapOf<String, BundleJar>()

    init {
        project.gradle.projectsEvaluated { _ ->
            if (project.plugins.hasPlugin(BundlePlugin.ID)) {
                bundle // forces default jar to be configured
            }
            bundles.values.forEach { it.projectsEvaluated() }
        }
    }

    val compose: Compose
        get() = compose(Compose.NAME)

    fun compose(taskName: String) = project.tasks.getByName(taskName) as Compose

    val composes: List<Compose>
        get() = project.tasks.withType(Compose::class.java).toList()

    fun compose(configurer: Compose.() -> Unit) = project.tasks.named(Compose.NAME, Compose::class.java, configurer)

    fun bundle(configurer: BundleJar.() -> Unit) = bundle(JavaPlugin.JAR_TASK_NAME, configurer)

    fun bundle(jarTaskName: String, configurer: BundleJar.() -> Unit) {
        project.tasks.withType(Jar::class.java)
                .named(jarTaskName)
                .configure { bundle(it, configurer) }
    }

    val bundle: BundleJar
        get() = bundle(JavaPlugin.JAR_TASK_NAME)

    fun bundle(jarTaskName: String) = bundle(project.tasks.getByName(jarTaskName) as Jar)

    fun bundle(jar: Jar, configurer: BundleJar.() -> Unit = {}): BundleJar {
        return bundleMap.getOrPut(jar.name) { BundleJar(aem, jar) }.apply(configurer)
    }

    /**
     * Contains OSGi bundle configuration used in case of composing CRX package.
     */
    @Nested
    val bundles: Map<String, BundleJar> = bundleMap

    fun pathed(path: String): TaskProvider<Task> {
        val projectPath = path.substringBeforeLast(":", project.path).ifEmpty { ":" }
        val taskName = path.substringAfterLast(":")

        return project.project(projectPath).tasks.named(taskName)
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

    fun named(name: String) = project.tasks.named(name)

    fun <T : Task> copy(name: String, suffix: String, type: Class<T>, configurer: T.() -> Unit = {}): TaskProvider<T> {
        return project.tasks.register("$name${suffix.capitalize()}", type) { task ->
            task.group = GROUP
            task.apply(configurer)
        }
    }

    fun await(suffix: String, configurer: Await.() -> Unit = {}) = copy(Await.NAME, suffix, Await::class.java, configurer)

    fun <T : Task> register(name: String, clazz: Class<T>): TaskProvider<T> {
        return register(name, clazz, Action {})
    }

    fun <T : Task> register(name: String, clazz: Class<T>, configurer: (T) -> Unit): TaskProvider<T> {
        return register(name, clazz, Action { configurer(it) })
    }

    fun <T : Task> register(name: String, clazz: Class<T>, configurer: Action<T>): TaskProvider<T> {
        with(project) {
            val provider = tasks.register(name, clazz, configurer)

            afterEvaluate { provider.configure { if (it is AemTask) it.projectEvaluated() } }
            gradle.projectsEvaluated { provider.configure { if (it is AemTask) it.projectsEvaluated() } }
            gradle.taskGraph.whenReady { graph -> provider.configure { if (it is AemTask) it.taskGraphReady(graph) } }

            return provider
        }
    }

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
                task.group = GROUP
                task.dependsOn(taskList).mustRunAfter(afterList)
            }
        }

        return sequence
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

    companion object {
        const val GROUP = "${AemTask.GROUP} (custom)"
    }
}