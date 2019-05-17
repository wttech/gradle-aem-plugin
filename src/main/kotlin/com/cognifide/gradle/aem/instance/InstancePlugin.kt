package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.common.AemPlugin
import com.cognifide.gradle.aem.common.tasks.lifecycle.*
import com.cognifide.gradle.aem.config.ConfigPlugin
import com.cognifide.gradle.aem.config.tasks.Resolve
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
        plugins.apply(ConfigPlugin::class.java)
    }

    @Suppress("LongMethod")
    private fun Project.setupTasks() {
        tasks {
            // Plugin tasks

            register<InstanceDown>(InstanceDown.NAME)
            register<InstanceUp>(InstanceUp.NAME) {
                mustRunAfter(InstanceDown.NAME, InstanceDestroy.NAME)
            }
            register<InstanceRestart>(InstanceRestart.NAME) {
                dependsOn(InstanceDown.NAME, InstanceUp.NAME)
            }
            register<InstanceCreateOnly>(InstanceCreateOnly.NAME) {
                dependsOn(Resolve.NAME)
                mustRunAfter(InstanceDestroy.NAME)
            }
            register<InstanceCreateAndUp>(InstanceCreateAndUp.NAME) {
                dependsOn(InstanceCreateOnly.NAME, InstanceUp.NAME)
            }
            register<InstanceDestroy>(InstanceDestroy.NAME) {
                dependsOn(InstanceDown.NAME)
            }
            register<InstanceSatisfy>(InstanceSatisfy.NAME) {
                dependsOn(Resolve.NAME).mustRunAfter(InstanceCreateAndUp.NAME)
            }
            register<InstanceReload>(InstanceReload.NAME) {
                mustRunAfter(InstanceSatisfy.NAME)
                plugins.withId(PackagePlugin.ID) { mustRunAfter(PackageDeploy.NAME) }
            }
            register<InstanceAwait>(InstanceAwait.NAME) {
                mustRunAfter(InstanceCreateAndUp.NAME, InstanceSatisfy.NAME)
                plugins.withId(PackagePlugin.ID) { mustRunAfter(PackageDeploy.NAME) }
            }
            register<InstanceCollect>(InstanceCollect.NAME) {
                mustRunAfter(InstanceSatisfy.NAME)
            }
            register<InstanceSetup>(InstanceSetup.NAME) {
                dependsOn(InstanceCreateAndUp.NAME, InstanceSatisfy.NAME)
                mustRunAfter(InstanceDestroy.NAME)
                plugins.withId(PackagePlugin.ID) { dependsOn(PackageDeploy.NAME) }
            }
            register<InstanceResetup>(InstanceResetup.NAME) {
                dependsOn(InstanceDestroy.NAME, InstanceSetup.NAME)
            }
            register<InstanceBackupOnly>(InstanceBackupOnly.NAME) {
                dependsOn(InstanceDown.NAME)
            }
            register<InstanceBackupAndUp>(InstanceBackupAndUp.NAME) {
                dependsOn(InstanceBackupOnly.NAME, InstanceUp.NAME)
            }
            register<InstanceRestoreOnly>(InstanceRestoreOnly.NAME) {
                dependsOn(Resolve.NAME, InstanceDestroy.NAME)
            }
            register<InstanceRestoreAndUp>(InstanceRestoreAndUp.NAME) {
                dependsOn(InstanceRestoreOnly.NAME, InstanceUp.NAME)
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

        const val FILES_PATH = "instance"
    }
}