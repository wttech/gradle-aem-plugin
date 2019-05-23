package com.cognifide.gradle.aem.common

import com.cognifide.gradle.aem.bundle.BundleJar
import com.cognifide.gradle.aem.common.tasks.Debug
import com.cognifide.gradle.aem.common.tasks.lifecycle.*
import com.cognifide.gradle.aem.environment.tasks.*
import com.cognifide.gradle.aem.instance.tasks.*
import com.cognifide.gradle.aem.pkg.tasks.*
import com.cognifide.gradle.aem.tooling.tasks.*
import com.cognifide.gradle.aem.tooling.tasks.Sync
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.Serializable
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test

@Suppress("TooManyFunctions")
class TaskFacade(private val aem: AemExtension) : Serializable {

    @JsonIgnore
    val project = aem.project

    private val bundleMap = mutableMapOf<String, BundleJar>()

    val bundles: List<BundleJar>
        get() = getAll(Jar::class.java).map { bundle(it) }

    internal fun jarsAsBundles() {
        initializeJarsAsBundles()
        project.afterEvaluate {
            finalizeJarsAsBundles()
        }
    }

    // Bundle plugin shorthands

    fun bundle(configurer: BundleJar.() -> Unit) = bundle(JavaPlugin.JAR_TASK_NAME, configurer)

    fun bundle(jarTaskName: String, configurer: BundleJar.() -> Unit) = named(jarTaskName, Jar::class.java) { bundle(this, configurer) }

    // Package plugin shorthands

    fun packageActivate(configurer: PackageActivate.() -> Unit) = named(PackageActivate.NAME, configurer)

    fun packageCompose(configurer: PackageCompose.() -> Unit) = named(PackageCompose.NAME, configurer)

    fun packageDelete(configurer: PackageDelete.() -> Unit) = named(PackageDelete.NAME, configurer)

    fun packageDeploy(configurer: PackageDeploy.() -> Unit) = named(PackageDeploy.NAME, configurer)

    fun packageInstall(configurer: PackageInstall.() -> Unit) = named(PackageInstall.NAME, configurer)

    fun packagePurge(configurer: PackagePurge.() -> Unit) = named(PackagePurge.NAME, configurer)

    fun packageUninstall(configurer: PackageUninstall.() -> Unit) = named(PackageUninstall.NAME, configurer)

    fun packageUpload(configurer: PackageUpload.() -> Unit) = named(PackageUpload.NAME, configurer)

    // Instance plugin shorthands

    fun instanceAwait(configurer: InstanceAwait.() -> Unit) = named(InstanceAwait.NAME, configurer)

    fun instanceBackup(configurer: InstanceBackup.() -> Unit) = named(InstanceBackup.NAME, configurer)

    fun instanceCollect(configurer: InstanceCollect.() -> Unit) = named(InstanceCollect.NAME, configurer)

    fun instanceCreate(configurer: InstanceCreate.() -> Unit) = named(InstanceCreate.NAME, configurer)

    fun instanceDestroy(configurer: InstanceDestroy.() -> Unit) = named(InstanceDestroy.NAME, configurer)

    fun instanceDown(configurer: InstanceDown.() -> Unit) = named(InstanceDown.NAME, configurer)

    fun instanceReload(configurer: InstanceReload.() -> Unit) = named(InstanceReload.NAME, configurer)

    fun instanceResetup(configurer: InstanceResetup.() -> Unit) = named(InstanceResetup.NAME, configurer)

    fun instanceRestart(configurer: InstanceRestart.() -> Unit) = named(InstanceRestart.NAME, configurer)

    fun instanceSatisfy(configurer: InstanceSatisfy.() -> Unit) = named(InstanceSatisfy.NAME, configurer)

    fun instanceSetup(configurer: InstanceSetup.() -> Unit) = named(InstanceSetup.NAME, configurer)

    fun instanceTail(configurer: InstanceTail.() -> Unit) = named(InstanceTail.NAME, configurer)

    fun instanceUp(configurer: InstanceUp.() -> Unit) = named(InstanceUp.NAME, configurer)

    // Environment plugin shorthands

    fun environmentDestroy(configurer: EnvironmentDestroy.() -> Unit) = named(EnvironmentDestroy.NAME, configurer)

    fun environmentDev(configurer: EnvironmentDev.() -> Unit) = named(EnvironmentDev.NAME, configurer)

    fun environmentDown(configurer: EnvironmentDown.() -> Unit) = named(EnvironmentDown.NAME, configurer)

    fun environmentHosts(configurer: EnvironmentHosts.() -> Unit) = named(EnvironmentHosts.NAME, configurer)

    fun environmentUp(configurer: EnvironmentUp.() -> Unit) = named(EnvironmentUp.NAME, configurer)

    fun environmentRestart(configurer: EnvironmentRestart.() -> Unit) = named(EnvironmentRestart.NAME, configurer)

    // Tooling plugin shorthands

    fun debug(configurer: Debug.() -> Unit) = named(Debug.NAME, configurer)

    fun rcp(configurer: Rcp.() -> Unit) = named(Rcp.NAME, configurer)

    fun sync(configurer: Sync.() -> Unit) = named(Sync.NAME, configurer)

    fun vlt(configurer: Vlt.() -> Unit) = named(Vlt.NAME, configurer)

    // Common lifecycle

    fun destroy(configurer: Destroy.() -> Unit) = registerOrConfigure(Destroy.NAME, configurer)

    fun down(configurer: Down.() -> Unit) = registerOrConfigure(Down.NAME, configurer)

    fun resetup(configurer: Resetup.() -> Unit) = registerOrConfigure(Resetup.NAME, configurer)

    fun restart(configurer: Restart.() -> Unit) = registerOrConfigure(Restart.NAME, configurer)

    fun setup(configurer: Setup.() -> Unit) = registerOrConfigure(Setup.NAME, configurer)

    fun up(configurer: Up.() -> Unit) = registerOrConfigure(Up.NAME, configurer)

    // Generic API & internals

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

    fun named(name: String): TaskProvider<Task> {
        return try {
            project.tasks.named(name)
        } catch (e: UnknownTaskException) {
            throw composeException(name)
        }
    }

    inline fun <reified T : Task> named(name: String, noinline configurer: T.() -> Unit = {}): TaskProvider<T> {
        return named(name, T::class.java, configurer)
    }

    fun <T : Task> named(name: String, type: Class<T>, configurer: T.() -> Unit): TaskProvider<T> {
        try {
            return project.tasks.named(name, type, configurer)
        } catch (e: UnknownTaskException) {
            throw composeException(name)
        }
    }

    inline fun <reified T : Task> typed(noinline configurer: T.() -> Unit = {}) {
        typed(T::class.java, configurer)
    }

    fun <T : Task> typed(type: Class<T>, configurer: T.() -> Unit) {
        project.tasks.withType(type).configureEach(configurer)
    }

    inline fun <reified T : Task> registerOrConfigure(name: String, noinline configurer: T.() -> Unit = {}): TaskProvider<T> {
        return try {
            project.tasks.named(name, T::class.java, configurer)
        } catch (e: UnknownTaskException) {
            register(name, T::class.java, configurer)
        }
    }

    inline fun <reified T : Task> registerOrConfigure(vararg names: String, noinline configurer: T.() -> Unit = {}) {
        names.forEach { registerOrConfigure(it, configurer) }
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

    fun sequence(name: String, sequenceOptions: TaskSequence.() -> Unit) = sequence(name, {}, sequenceOptions)

    fun sequence(name: String, taskOptions: Task.() -> Unit, sequenceOptions: TaskSequence.() -> Unit): TaskProvider<Task> {
        val sequence = project.tasks.register(name)

        project.gradle.projectsEvaluated { _ ->
            val options = TaskSequence().apply(sequenceOptions)
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
                classpath += project.files(jar.archiveFile.get().asFile)
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
}