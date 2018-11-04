package com.cognifide.gradle.aem.base

import com.cognifide.gradle.aem.api.AemExtension
import com.cognifide.gradle.aem.api.AemPlugin
import com.cognifide.gradle.aem.base.debug.DebugTask
import com.cognifide.gradle.aem.base.download.DownloadTask
import com.cognifide.gradle.aem.base.vlt.*
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * Provides configuration used by both package and instance plugins.
 */
class BasePlugin : AemPlugin() {

    override fun Project.configure() {
        setupGreet()
        setupDependentPlugins()
        setupExtensions()
        setupTasks()
    }

    private fun Project.setupGreet() {
        AemPlugin.once { logger.info("Using: ${AemPlugin.NAME_WITH_VERSION}") }
    }

    private fun Project.setupDependentPlugins() {
        plugins.apply(BasePlugin::class.java)
    }

    private fun Project.setupExtensions() {
        extensions.create(AemExtension.NAME, AemExtension::class.java, this)
    }

    private fun Project.setupTasks() {
        registerTask(DebugTask.NAME, DebugTask::class.java)
        registerTask(RcpTask.NAME, RcpTask::class.java)
        registerTask(CleanTask.NAME, CleanTask::class.java) {
            it.mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME, CheckoutTask.NAME, DownloadTask.NAME)
        }
        registerTask(VltTask.NAME, VltTask::class.java) {
            it.mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
        }
        registerTask(CheckoutTask.NAME, CheckoutTask::class.java) {
            it.mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
        }
        registerTask(SyncTask.NAME, SyncTask::class.java) {
            it.dependsOn(LifecycleBasePlugin.CLEAN_TASK_NAME, CleanTask.NAME)
            it.dependsOn(CheckoutTask.NAME)
        }
        registerTask(DownloadTask.NAME, DownloadTask::class.java) {
            it.mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
        }
    }

    companion object {
        const val PKG = "com.cognifide.gradle.aem"

        const val ID = "com.cognifide.aem.base"
    }

}