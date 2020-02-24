package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.instance.tasks.InstanceProvision
import com.cognifide.gradle.aem.instance.rcp.InstanceRcp
import com.cognifide.gradle.aem.instance.tasks.InstanceSatisfy
import com.cognifide.gradle.aem.instance.tasks.InstanceTail
import com.cognifide.gradle.aem.instance.tasks.*
import com.cognifide.gradle.common.CommonDefaultPlugin
import com.cognifide.gradle.common.RuntimePlugin
import com.cognifide.gradle.common.tasks.runtime.*
import org.gradle.api.Project

class LocalInstancePlugin : CommonDefaultPlugin() {

    override fun Project.configureProject() {
        setupDependentPlugins()
        setupTasks()
    }

    private fun Project.setupDependentPlugins() {
        plugins.apply(InstancePlugin::class.java)
        plugins.apply(RuntimePlugin::class.java)
    }

    @Suppress("LongMethod")
    private fun Project.setupTasks() = tasks {

            // Plugin tasks

            register<InstanceDown>(InstanceDown.NAME)
            register<InstanceUp>(InstanceUp.NAME) {
                dependsOn(InstanceCreate.NAME)
                mustRunAfter(InstanceDown.NAME, InstanceDestroy.NAME)
            }
            register<InstanceRestart>(InstanceRestart.NAME) {
                dependsOn(InstanceDown.NAME, InstanceUp.NAME)
            }
            register<InstanceCreate>(InstanceCreate.NAME) {
                mustRunAfter(InstanceDestroy.NAME, InstanceResolve.NAME)
            }
            register<InstanceDestroy>(InstanceDestroy.NAME) {
                dependsOn(InstanceDown.NAME)
            }
            named<InstanceSatisfy>(InstanceSatisfy.NAME) {
                mustRunAfter(InstanceResolve.NAME, InstanceCreate.NAME, InstanceUp.NAME)
            }
            named<InstanceProvision>(InstanceProvision.NAME) {
                mustRunAfter(InstanceResolve.NAME, InstanceCreate.NAME, InstanceUp.NAME)
            }
            named<InstanceAwait>(InstanceAwait.NAME) {
                mustRunAfter(InstanceCreate.NAME, InstanceUp.NAME)
            }
            named<InstanceSetup>(InstanceSetup.NAME) {
                dependsOn(InstanceCreate.NAME, InstanceUp.NAME)
                mustRunAfter(InstanceDestroy.NAME)
            }
            register<InstanceResetup>(InstanceResetup.NAME) {
                dependsOn(InstanceDestroy.NAME, InstanceSetup.NAME)
            }
            register<InstanceBackup>(InstanceBackup.NAME) {
                mustRunAfter(InstanceDown.NAME)
            }
            register<InstanceResolve>(InstanceResolve.NAME)
            named<InstanceTail>(InstanceTail.NAME) {
                mustRunAfter(InstanceResolve.NAME, InstanceCreate.NAME, InstanceUp.NAME)
            }
            named<InstanceRcp>(InstanceRcp.NAME) {
                mustRunAfter(InstanceResolve.NAME, InstanceCreate.NAME, InstanceUp.NAME)
            }
            named<InstanceGroovyEval>(InstanceGroovyEval.NAME) {
                mustRunAfter(InstanceResolve.NAME, InstanceCreate.NAME, InstanceUp.NAME)
            }

            // Runtime lifecycle

            named<Up>(Up.NAME) {
                dependsOn(InstanceUp.NAME)
                mustRunAfter(InstanceBackup.NAME)
            }
            named<Down>(Down.NAME) {
                dependsOn(InstanceDown.NAME)
            }
            named<Destroy>(Destroy.NAME) {
                dependsOn(InstanceDestroy.NAME)
            }
            named<Restart>(Restart.NAME) {
                dependsOn(InstanceRestart.NAME)
            }
            named<Setup>(Setup.NAME) {
                dependsOn(InstanceSetup.NAME)
            }
            named<Resetup>(Resetup.NAME) {
                dependsOn(InstanceResetup.NAME)
            }
            named<Resolve>(Resolve.NAME) {
                dependsOn(InstanceResolve.NAME)
            }
            named<Await>(Await.NAME) {
                dependsOn(InstanceAwait.NAME)
            }
        }

    companion object {
        const val ID = "com.cognifide.aem.instance.local"
    }
}
