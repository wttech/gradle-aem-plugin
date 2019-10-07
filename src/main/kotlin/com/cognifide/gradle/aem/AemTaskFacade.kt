package com.cognifide.gradle.aem

import com.cognifide.gradle.aem.bundle.tasks.BundleCompose
import com.cognifide.gradle.aem.common.tasks.Debug
import com.cognifide.gradle.aem.common.tasks.TaskSequence
import com.cognifide.gradle.aem.common.tasks.lifecycle.*
import com.cognifide.gradle.aem.environment.tasks.*
import com.cognifide.gradle.aem.instance.provision.InstanceProvision
import com.cognifide.gradle.aem.instance.satisfy.InstanceSatisfy
import com.cognifide.gradle.aem.instance.tail.InstanceTail
import com.cognifide.gradle.aem.instance.tasks.*
import com.cognifide.gradle.aem.pkg.tasks.*
import com.cognifide.gradle.aem.tooling.rcp.Rcp
import com.cognifide.gradle.aem.tooling.sync.Sync
import com.cognifide.gradle.aem.tooling.vlt.Vlt
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.Serializable
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.tasks.TaskProvider

@Suppress("TooManyFunctions")
class AemTaskFacade(private val aem: AemExtension) : Serializable {

    @JsonIgnore
    val project = aem.project

    private val bundleMap = mutableMapOf<String, BundleCompose>()

    // Bundle plugin shorthands

    val bundles get() = getAll(BundleCompose::class.java)

    fun bundleCompose(configurer: BundleCompose.() -> Unit) = named(BundleCompose.NAME, configurer)

    // Package plugin shorthands

    val packages get() = getAll(PackageCompose::class.java)

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

    fun instanceCreate(configurer: InstanceCreate.() -> Unit) = named(InstanceCreate.NAME, configurer)

    fun instanceDestroy(configurer: InstanceDestroy.() -> Unit) = named(InstanceDestroy.NAME, configurer)

    fun instanceDown(configurer: InstanceDown.() -> Unit) = named(InstanceDown.NAME, configurer)

    fun instanceProvision(configurer: InstanceProvision.() -> Unit) = named(InstanceProvision.NAME, configurer)

    fun instanceReload(configurer: InstanceReload.() -> Unit) = named(InstanceReload.NAME, configurer)

    fun instanceResetup(configurer: InstanceResetup.() -> Unit) = named(InstanceResetup.NAME, configurer)

    fun instanceRestart(configurer: InstanceRestart.() -> Unit) = named(InstanceRestart.NAME, configurer)

    fun instanceSatisfy(configurer: InstanceSatisfy.() -> Unit) = named(InstanceSatisfy.NAME, configurer)

    fun instanceSetup(configurer: InstanceSetup.() -> Unit) = named(InstanceSetup.NAME, configurer)

    fun instanceTail(configurer: InstanceTail.() -> Unit) = named(InstanceTail.NAME, configurer)

    fun instanceUp(configurer: InstanceUp.() -> Unit) = named(InstanceUp.NAME, configurer)

    // Environment plugin shorthands

    fun environmentAwait(configurer: EnvironmentAwait.() -> Unit) = named(EnvironmentAwait.NAME, configurer)

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

    inline fun <reified T : Task> typed(noinline configurer: T.() -> Unit) {
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

    fun registerSequence(name: String, sequenceOptions: TaskSequence.() -> Unit) = registerSequence(name, {}, sequenceOptions)

    fun registerSequence(name: String, taskOptions: Task.() -> Unit, sequenceOptions: TaskSequence.() -> Unit): TaskProvider<Task> {
        return project.tasks.register(name) { task ->
            val options = TaskSequence().apply(sequenceOptions)

            task.group = AemTask.GROUP
            task.dependsOn(options.dependentTasks).mustRunAfter(options.afterTasks)
            task.apply(taskOptions)

            val dependentTasks = pathed(options.dependentTasks)
            val afterTasks = pathed(options.afterTasks)

            if (dependentTasks.size > 1) {
                for (i in 1 until dependentTasks.size) {
                    val previous = dependentTasks[i - 1]
                    val current = dependentTasks[i]

                    current.configure { it.mustRunAfter(previous) }
                }
            }
            dependentTasks.forEach { dependentTask ->
                dependentTask.configure { it.mustRunAfter(afterTasks) }
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
