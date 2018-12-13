package com.cognifide.gradle.aem.config

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.AemPlugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin

/**
 * Provides configuration used by both package and instance plugins.
 */
class ConfigPlugin : AemPlugin() {

    override fun Project.configure() {
        setupGreet()
        setupDependentPlugins()
        setupExtensions()
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

    companion object {
        const val ID = "com.cognifide.aem.config"
    }
}