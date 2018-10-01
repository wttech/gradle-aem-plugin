package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.base.BasePlugin
import com.cognifide.gradle.aem.instance.CreateTask
import com.cognifide.gradle.aem.instance.InstancePlugin
import com.cognifide.gradle.aem.instance.SatisfyTask
import com.cognifide.gradle.aem.instance.UpTask
import com.cognifide.gradle.aem.pkg.deploy.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

class PackagePlugin : Plugin<Project> {

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
        tasks.register(PrepareTask.NAME, PrepareTask::class.java) {
            it.mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
        }
        tasks.register(ComposeTask.NAME, ComposeTask::class.java) {
            it.dependsOn(PrepareTask.NAME, LifecycleBasePlugin.ASSEMBLE_TASK_NAME, LifecycleBasePlugin.CHECK_TASK_NAME)
            it.mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
        }
        tasks.register(UploadTask.NAME, UploadTask::class.java) {
            it.dependsOn(ComposeTask.NAME)
        }
        tasks.register(DeleteTask.NAME, DeleteTask::class.java)
        tasks.register(PurgeTask.NAME, PurgeTask::class.java)
        tasks.register(InstallTask.NAME, InstallTask::class.java) {
            it.mustRunAfter(ComposeTask.NAME, UploadTask.NAME)
        }
        tasks.register(UninstallTask.NAME, UninstallTask::class.java)
        tasks.register(ActivateTask.NAME, ActivateTask::class.java) {
            it.mustRunAfter(ComposeTask.NAME, UploadTask.NAME, InstallTask.NAME)
        }
        tasks.register(DeployTask.NAME, DeployTask::class.java) {
            it.dependsOn(ComposeTask.NAME)
        }
        tasks.named(LifecycleBasePlugin.BUILD_TASK_NAME).configure { it.dependsOn(ComposeTask.NAME) }

        plugins.withId(InstancePlugin.ID) {
            tasks.named(DeployTask.NAME).configure { task ->
                task.mustRunAfter(CreateTask.NAME, UpTask.NAME, SatisfyTask.NAME)
            }
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.package"

        const val VLT_PATH = "META-INF/vault"

        const val VLT_PROPERTIES = "$VLT_PATH/properties.xml"

        const val JCR_ROOT = "jcr_root"
    }

}