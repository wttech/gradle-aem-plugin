package com.cognifide.gradle.aem.environment

import com.cognifide.gradle.aem.common.AemPlugin
import com.cognifide.gradle.aem.common.tasks.lifecycle.Destroy
import com.cognifide.gradle.aem.common.tasks.lifecycle.Down
import com.cognifide.gradle.aem.common.tasks.lifecycle.Restart
import com.cognifide.gradle.aem.common.tasks.lifecycle.Up
import com.cognifide.gradle.aem.config.ConfigPlugin
import com.cognifide.gradle.aem.environment.tasks.*
import com.cognifide.gradle.aem.instance.InstancePlugin
import com.cognifide.gradle.aem.instance.tasks.InstanceUp
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

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
        tasks {
            register<EnvironmentDev>(EnvironmentDev.NAME)
            register<EnvironmentHosts>(EnvironmentHosts.NAME)
            register<EnvironmentUp>(EnvironmentUp.NAME) {
                mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME, EnvironmentDown.NAME)
                plugins.withId(InstancePlugin.ID) { mustRunAfter(InstanceUp.NAME) }
            }
            register<EnvironmentDown>(EnvironmentDown.NAME)

            register<EnvironmentDestroy>(EnvironmentDestroy.NAME) {
                dependsOn(EnvironmentDown.NAME)
            }
            register<EnvironmentRestart>(EnvironmentRestart.NAME) {
                dependsOn(EnvironmentDown.NAME, EnvironmentUp.NAME)
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
            registerOrConfigure<Restart>(Restart.NAME) {
                dependsOn(EnvironmentRestart.NAME)
            }
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.environment"
    }
}