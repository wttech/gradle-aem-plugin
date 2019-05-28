package com.cognifide.gradle.aem.tooling

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.common.CommonPlugin
import com.cognifide.gradle.aem.tooling.rcp.Rcp
import com.cognifide.gradle.aem.tooling.sync.Sync
import com.cognifide.gradle.aem.tooling.vlt.Vlt
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
        plugins.apply(CommonPlugin::class.java)
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