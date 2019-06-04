package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.common.CommonPlugin
import com.cognifide.gradle.aem.common.tasks.Resolve
import com.cognifide.gradle.aem.common.tasks.lifecycle.*
import com.cognifide.gradle.aem.instance.satisfy.InstanceSatisfy
import com.cognifide.gradle.aem.instance.tail.InstanceTail
import com.cognifide.gradle.aem.instance.tasks.*
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.pkg.tasks.PackageDeploy
import org.gradle.api.Project

/**
 * Separate plugin which provides tasks for managing local instances.
 * Most often should be applied only to one project in build.
 * Applying it multiple times to same configuration could case confusing errors like AEM started multiple times.
 */
class InstancePlugin : AemPlugin() {

    override fun Project.configure() {
        setupDependentPlugins()
        setupTasks()
    }

    private fun Project.setupDependentPlugins() {
        plugins.apply(CommonPlugin::class.java)
    }

    @Suppress("LongMethod")
    private fun Project.setupTasks() {
        tasks {
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
                dependsOn(Resolve.NAME)
                mustRunAfter(InstanceDestroy.NAME)
            }
            register<InstanceDestroy>(InstanceDestroy.NAME) {
                dependsOn(InstanceDown.NAME)
            }
            register<InstanceSatisfy>(InstanceSatisfy.NAME) {
                dependsOn(Resolve.NAME).mustRunAfter(InstanceCreate.NAME, InstanceUp.NAME)
            }
            register<InstanceReload>(InstanceReload.NAME) {
                mustRunAfter(InstanceSatisfy.NAME)
                plugins.withId(PackagePlugin.ID) { mustRunAfter(PackageDeploy.NAME) }
            }
            register<InstanceCheck>(InstanceCheck.NAME) {
                mustRunAfter(InstanceCreate.NAME, InstanceUp.NAME, InstanceSatisfy.NAME)
                plugins.withId(PackagePlugin.ID) { mustRunAfter(PackageDeploy.NAME) }
            }
            register<InstanceSetup>(InstanceSetup.NAME) {
                dependsOn(InstanceCreate.NAME, InstanceUp.NAME, InstanceSatisfy.NAME)
                mustRunAfter(InstanceDestroy.NAME)
                plugins.withId(PackagePlugin.ID) { dependsOn(PackageDeploy.NAME) }
            }
            register<InstanceResetup>(InstanceResetup.NAME) {
                dependsOn(InstanceDestroy.NAME, InstanceSetup.NAME)
            }
            register<InstanceBackup>(InstanceBackup.NAME) {
                dependsOn(InstanceDown.NAME)
                finalizedBy(InstanceUp.NAME)
            }

            register<InstanceTail>(InstanceTail.NAME)

            // Common lifecycle

            registerOrConfigure<Up>(Up.NAME) {
                dependsOn(InstanceUp.NAME)
            }
            registerOrConfigure<Down>(Down.NAME) {
                dependsOn(InstanceDown.NAME)
            }
            registerOrConfigure<Destroy>(Destroy.NAME) {
                dependsOn(InstanceDestroy.NAME)
            }
            registerOrConfigure<Restart>(Restart.NAME) {
                dependsOn(InstanceRestart.NAME)
            }
            registerOrConfigure<Setup>(Setup.NAME) {
                dependsOn(InstanceSetup.NAME)
            }
            registerOrConfigure<Resetup>(Resetup.NAME) {
                dependsOn(InstanceResetup.NAME)
            }
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.instance"
    }
}