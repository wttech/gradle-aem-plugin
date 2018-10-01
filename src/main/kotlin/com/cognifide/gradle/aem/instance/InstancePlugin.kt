package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.base.BasePlugin
import com.cognifide.gradle.aem.pkg.ComposeTask
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.pkg.deploy.DeployTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * Separate plugin which provides tasks for managing local instances.
 * Most often should be applied only to one project in build.
 * Applying it multiple times to same configuration could case confusing errors like AEM started multiple times.
 */
class InstancePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        with(project) {
            setupDependentPlugins()
            setupTasks()
        }
    }

    private fun Project.setupDependentPlugins() {
        plugins.apply(BasePlugin::class.java)
    }

    private fun Project.setupTasks() {
        tasks.register(ResolveTask.NAME, ResolveTask::class.java) {
            it.mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
        }
        tasks.register(DownTask.NAME, DownTask::class.java)
        tasks.register(UpTask.NAME, UpTask::class.java) {
            it.dependsOn(CreateTask.NAME).mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME, DownTask.NAME)
        }
        tasks.register(RestartTask.NAME, RestartTask::class.java) {
            it.dependsOn(DownTask.NAME, UpTask.NAME)
        }
        tasks.register(CreateTask.NAME, CreateTask::class.java) {
            it.dependsOn(ResolveTask.NAME).mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
        }
        tasks.register(DestroyTask.NAME, DestroyTask::class.java) {
            it.dependsOn(DownTask.NAME)
        }
        tasks.register(SatisfyTask.NAME, SatisfyTask::class.java) {
            it.dependsOn(ResolveTask.NAME).mustRunAfter(CreateTask.NAME, UpTask.NAME)
        }
        tasks.register(ReloadTask.NAME, ReloadTask::class.java) {
            it.mustRunAfter(SatisfyTask.NAME, DeployTask.NAME)
        }
        tasks.register(AwaitTask.NAME, AwaitTask::class.java) {
            it.mustRunAfter(CreateTask.NAME, UpTask.NAME, SatisfyTask.NAME, DeployTask.NAME)
        }
        tasks.register(CollectTask.NAME, CollectTask::class.java) { task ->
            task.mustRunAfter(SatisfyTask.NAME)
            gradle.projectsEvaluated { // TODO ?
                allprojects.forEach { subproject ->
                    if (subproject.plugins.hasPlugin(PackagePlugin.ID)) {
                        task.dependsOn(ComposeTask.NAME)
                    }
                }
            }
        }
        tasks.register(SetupTask.NAME, SetupTask::class.java) { task ->
            task.dependsOn(CreateTask.NAME, UpTask.NAME, SatisfyTask.NAME).mustRunAfter(DestroyTask.NAME)
            plugins.withId(PackagePlugin.ID) { task.dependsOn(DeployTask.NAME) }
        }
        tasks.register(ResetupTask.NAME, ResetupTask::class.java) {
            it.dependsOn(DestroyTask.NAME, SetupTask.NAME)
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.instance"

        const val FILES_PATH = "local-instance"
    }

}