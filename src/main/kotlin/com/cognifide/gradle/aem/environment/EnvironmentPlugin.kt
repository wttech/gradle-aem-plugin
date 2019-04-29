package com.cognifide.gradle.aem.environment

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.AemPlugin
import com.cognifide.gradle.aem.config.ConfigPlugin
import com.cognifide.gradle.aem.config.tasks.Destroy
import com.cognifide.gradle.aem.config.tasks.Down
import com.cognifide.gradle.aem.config.tasks.Up
import com.cognifide.gradle.aem.environment.tasks.*
import com.cognifide.gradle.aem.instance.InstancePlugin
import com.cognifide.gradle.aem.instance.tasks.InstanceUp
import org.gradle.api.Project

/**
 * Separate plugin which provides tasks for managing local development environment additional to AEM, like:
 * Dispatcher, Solr, Knot.X, etc.
 *
 * Most often should be applied only to one project in build.
 */
class EnvironmentPlugin : AemPlugin() {

    override fun Project.configure() {
        setupDependentPlugins()
        setupTasks()
    }

    private fun Project.setupDependentPlugins() {
        plugins.apply(ConfigPlugin::class.java)
    }

    private fun Project.setupTasks() {
        with(AemExtension.of(this).tasks) {
            register<EnvironmentDev>(EnvironmentDev.NAME)
            register<EnvironmentHosts>(EnvironmentHosts.NAME)
            register<EnvironmentUp>(EnvironmentUp.NAME) {
                plugins.withId(InstancePlugin.ID) { mustRunAfter(InstanceUp.NAME) }
            }
            register<EnvironmentDown>(EnvironmentDown.NAME)

            register<EnvironmentDestroy>(EnvironmentDestroy.NAME) {
                dependsOn(EnvironmentDown.NAME)
            }
            register<EnvironmentRestart>(EnvironmentRestart.NAME) {
                dependsOn(EnvironmentDown.NAME)
                finalizedBy(EnvironmentUp.NAME)
            }
            registerOrConfigure<Up>(Up.NAME) {
                dependsOn(EnvironmentUp.NAME)
            }
            registerOrConfigure<Down>(Down.NAME) {
                dependsOn(EnvironmentDown.NAME)
            }
            registerOrConfigure<Destroy>(Destroy.NAME) {
                dependsOn(EnvironmentDestroy.NAME)
            }
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.environment"
    }
}