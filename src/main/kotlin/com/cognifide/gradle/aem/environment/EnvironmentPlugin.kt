package com.cognifide.gradle.aem.environment

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.AemPlugin
import com.cognifide.gradle.aem.environment.tasks.EnvDown
import com.cognifide.gradle.aem.environment.tasks.EnvSetup
import com.cognifide.gradle.aem.environment.tasks.EnvUp
import com.cognifide.gradle.aem.instance.InstancePlugin
import com.cognifide.gradle.aem.instance.tasks.Setup
import org.gradle.api.Project

/**
 * Separate plugin which provides tasks for managing local development environment additional to AEM,
 * like Dispatcher, Solr, Knot.X, etc.
 * Most often should be applied only to one project in build.
 */
class EnvironmentPlugin : AemPlugin() {

    override fun Project.configure() {
        setupDependentPlugins()
        setupTasks()
    }

    private fun Project.setupDependentPlugins() {
        plugins.apply(InstancePlugin::class.java)
    }

    private fun Project.setupTasks() {
        with(AemExtension.of(this).tasks) {
            register<EnvUp>(EnvUp.NAME)
            register<EnvDown>(EnvDown.NAME)
            register<EnvSetup>(EnvSetup.NAME) {
                dependsOn(Setup.NAME)
                finalizedBy(EnvUp.NAME)
            }
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.environment"
    }
}