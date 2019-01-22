package com.cognifide.gradle.aem.tooling

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.AemPlugin
import com.cognifide.gradle.aem.config.ConfigPlugin
import com.cognifide.gradle.aem.tooling.tasks.*
import com.cognifide.gradle.aem.tooling.tasks.Debug
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * Provides configuration used by both package and instance plugins.
 */
class ToolingPlugin : AemPlugin() {

    override fun Project.configure() {
        setupDependentPlugins()
        setupTasks()
    }

    private fun Project.setupDependentPlugins() {
        plugins.apply(ConfigPlugin::class.java)
    }

    private fun Project.setupTasks() {
        with(AemExtension.of(this).tasks) {
            register<Debug>(Debug.NAME) {
                dependsOn(LifecycleBasePlugin.BUILD_TASK_NAME)
            }
            register<Rcp>(Rcp.NAME)
            register<Vlt>(Vlt.NAME) {
                mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
            }
            register<Sync>(Sync.NAME) {
                mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
            }
            register<Tail>(Tail.NAME)
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.tooling"
    }
}