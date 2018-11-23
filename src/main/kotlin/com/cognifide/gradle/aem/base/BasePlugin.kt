package com.cognifide.gradle.aem.base

import com.cognifide.gradle.aem.api.AemExtension
import com.cognifide.gradle.aem.api.AemPlugin
import com.cognifide.gradle.aem.base.tasks.*
import com.cognifide.gradle.aem.base.tasks.Debug
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
        with(TaskFactory(this)) {
            register(Debug.NAME, Debug::class.java) {
                it.dependsOn(LifecycleBasePlugin.BUILD_TASK_NAME)
            }
            register(Rcp.NAME, Rcp::class.java)
            register(Clean.NAME, Clean::class.java) {
                it.mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME, Checkout.NAME)
            }
            register(Vlt.NAME, Vlt::class.java) {
                it.mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
            }
            register(Checkout.NAME, Checkout::class.java) {
                it.mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
            }
            register(Sync.NAME, Sync::class.java) {
                it.mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
                it.dependsOn(Clean.NAME, Checkout.NAME)
            }
        }
    }

    companion object {
        const val PKG = "com.cognifide.gradle.aem"

        const val ID = "com.cognifide.aem.base"
    }
}