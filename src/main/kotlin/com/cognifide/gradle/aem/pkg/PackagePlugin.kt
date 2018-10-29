package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.api.AemPlugin
import com.cognifide.gradle.aem.base.BasePlugin
import com.cognifide.gradle.aem.instance.CreateTask
import com.cognifide.gradle.aem.instance.InstancePlugin
import com.cognifide.gradle.aem.instance.SatisfyTask
import com.cognifide.gradle.aem.instance.UpTask
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

class PackagePlugin : AemPlugin() {

    override fun Project.configure() {
        setupDependentPlugins()
        setupTasks()
    }

    private fun Project.setupDependentPlugins() {
        plugins.apply(BasePlugin::class.java)
    }

    private fun Project.setupTasks() {
        registerTask(ComposeTask.NAME, ComposeTask::class.java) {
            it.dependsOn(LifecycleBasePlugin.ASSEMBLE_TASK_NAME, LifecycleBasePlugin.CHECK_TASK_NAME)
            it.mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
        }
        registerTask(DeleteTask.NAME, DeleteTask::class.java)
        registerTask(PurgeTask.NAME, PurgeTask::class.java)
        registerTask(UninstallTask.NAME, UninstallTask::class.java)
        registerTask(ActivateTask.NAME, ActivateTask::class.java) {
            it.mustRunAfter(ComposeTask.NAME)
        }
        registerTask(DeployTask.NAME, DeployTask::class.java) {
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