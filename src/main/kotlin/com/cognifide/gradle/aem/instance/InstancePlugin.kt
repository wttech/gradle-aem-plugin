package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemPlugin
import com.cognifide.gradle.aem.base.BasePlugin
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.pkg.deploy.DeployTask
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * Separate plugin which provides tasks for managing local instances.
 * Most often should be applied only to one project in build.
 * Applying it multiple times to same configuration could case confusing errors like AEM started multiple times.
 */
class InstancePlugin : AemPlugin() {
    
    override fun Project.configure() {
        setupDependentPlugins()
        setupTasks()
    }

    private fun Project.setupDependentPlugins() {
        plugins.apply(BasePlugin::class.java)
    }

    private fun Project.setupTasks() {
        registerTask(ResolveTask.NAME, ResolveTask::class.java) {
            it.mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
        }
        registerTask(DownTask.NAME, DownTask::class.java)
        registerTask(UpTask.NAME, UpTask::class.java) {
            it.dependsOn(CreateTask.NAME).mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME, DownTask.NAME)
        }
        registerTask(RestartTask.NAME, RestartTask::class.java) {
            it.dependsOn(DownTask.NAME, UpTask.NAME)
        }
        registerTask(CreateTask.NAME, CreateTask::class.java) {
            it.dependsOn(ResolveTask.NAME).mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
        }
        registerTask(DestroyTask.NAME, DestroyTask::class.java) {
            it.dependsOn(DownTask.NAME)
        }
        registerTask(SatisfyTask.NAME, SatisfyTask::class.java) {
            it.dependsOn(ResolveTask.NAME).mustRunAfter(CreateTask.NAME, UpTask.NAME)
        }
        registerTask(ReloadTask.NAME, ReloadTask::class.java) {
            it.mustRunAfter(SatisfyTask.NAME, DeployTask.NAME)
        }
        registerTask(AwaitTask.NAME, AwaitTask::class.java) {
            it.mustRunAfter(CreateTask.NAME, UpTask.NAME, SatisfyTask.NAME, DeployTask.NAME)
        }
        registerTask(CollectTask.NAME, CollectTask::class.java) { task ->
            task.mustRunAfter(SatisfyTask.NAME)
        }
        registerTask(SetupTask.NAME, SetupTask::class.java) { task ->
            task.dependsOn(CreateTask.NAME, UpTask.NAME, SatisfyTask.NAME).mustRunAfter(DestroyTask.NAME)
            plugins.withId(PackagePlugin.ID) { task.dependsOn(DeployTask.NAME) }
        }
        registerTask(ResetupTask.NAME, ResetupTask::class.java) {
            it.dependsOn(DestroyTask.NAME, SetupTask.NAME)
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.instance"

        const val FILES_PATH = "local-instance"
    }

}