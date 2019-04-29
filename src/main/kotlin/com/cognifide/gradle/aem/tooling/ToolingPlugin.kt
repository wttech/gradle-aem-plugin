package com.cognifide.gradle.aem.tooling

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.AemPlugin
import com.cognifide.gradle.aem.config.ConfigPlugin
import com.cognifide.gradle.aem.tooling.tasks.Rcp
import com.cognifide.gradle.aem.tooling.tasks.Sync
import com.cognifide.gradle.aem.tooling.tasks.Vlt
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * Provides tasks useful even when working without CRX package source files.
 *
 * E.g apply this plugin to projects in which using AEM sync is appropriate.
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
            register<Rcp>(Rcp.NAME)
            register<Vlt>(Vlt.NAME) {
                mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
            }
            register<Sync>(Sync.NAME) {
                mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
            }
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.tooling"
    }
}