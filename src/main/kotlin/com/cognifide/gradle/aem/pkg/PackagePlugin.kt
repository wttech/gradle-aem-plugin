package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.base.BasePlugin
import com.cognifide.gradle.aem.base.TaskFactory
import com.cognifide.gradle.aem.common.AemPlugin
import com.cognifide.gradle.aem.instance.InstancePlugin
import com.cognifide.gradle.aem.instance.tasks.Create
import com.cognifide.gradle.aem.instance.tasks.Satisfy
import com.cognifide.gradle.aem.instance.tasks.Up
import com.cognifide.gradle.aem.pkg.tasks.*
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
        with(TaskFactory(this)) {
            register(Compose.NAME, Compose::class.java) { task ->
                task.dependsOn(LifecycleBasePlugin.ASSEMBLE_TASK_NAME, LifecycleBasePlugin.CHECK_TASK_NAME)
                task.mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
            }
            register(Upload.NAME, Upload::class.java) { task ->
                task.dependsOn(Compose.NAME)
            }
            register(Install.NAME, Install::class.java) { task ->
                task.dependsOn(Compose.NAME)
                task.mustRunAfter(Upload.NAME)
            }
            register(Uninstall.NAME, Uninstall::class.java) { task ->
                task.dependsOn(Compose.NAME)
                task.mustRunAfter(Upload.NAME, Install.NAME)
            }
            register(Activate.NAME, Activate::class.java) { task ->
                task.dependsOn(Compose.NAME)
                task.mustRunAfter(Upload.NAME, Install.NAME)
            }
            register(Deploy.NAME, Deploy::class.java) { task ->
                task.dependsOn(Compose.NAME)
            }
            register(Delete.NAME, Delete::class.java) { task ->
                task.dependsOn(Compose.NAME)
                task.mustRunAfter(Deploy.NAME, Upload.NAME, Install.NAME, Activate.NAME, Uninstall.NAME)
            }
            register(Purge.NAME, Purge::class.java) { task ->
                task.dependsOn(Compose.NAME)
                task.mustRunAfter(Deploy.NAME, Upload.NAME, Install.NAME, Activate.NAME, Uninstall.NAME)
            }
        }

        tasks.named(LifecycleBasePlugin.BUILD_TASK_NAME).configure { it.dependsOn(Compose.NAME) }

        plugins.withId(InstancePlugin.ID) {
            tasks.named(Deploy.NAME).configure { task ->
                task.mustRunAfter(Create.NAME, Up.NAME, Satisfy.NAME)
            }
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.package"
    }
}