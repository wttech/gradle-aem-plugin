package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.api.AemPlugin
import com.cognifide.gradle.aem.base.BasePlugin
import com.cognifide.gradle.aem.instance.tasks.Create
import com.cognifide.gradle.aem.instance.InstancePlugin
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
        registerTask(Compose.NAME, Compose::class.java) {
            it.dependsOn(LifecycleBasePlugin.ASSEMBLE_TASK_NAME, LifecycleBasePlugin.CHECK_TASK_NAME)
            it.mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
        }
        registerTask(Delete.NAME, Delete::class.java)
        registerTask(Purge.NAME, Purge::class.java)
        registerTask(Uninstall.NAME, Uninstall::class.java)
        registerTask(Activate.NAME, Activate::class.java) {
            it.mustRunAfter(Compose.NAME)
        }
        registerTask(Deploy.NAME, Deploy::class.java) {
            it.dependsOn(Compose.NAME)
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

        const val VLT_PATH = "META-INF/vault"

        const val VLT_HOOKS_PATH = "META-INF/vault/hooks"

        const val VLT_PROPERTIES = "$VLT_PATH/properties.xml"

        const val JCR_ROOT = "jcr_root"
    }

}