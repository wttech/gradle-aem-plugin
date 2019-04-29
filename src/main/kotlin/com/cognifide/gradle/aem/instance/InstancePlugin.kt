package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.AemPlugin
import com.cognifide.gradle.aem.config.ConfigPlugin
import com.cognifide.gradle.aem.config.tasks.Resolve
import com.cognifide.gradle.aem.instance.tasks.*
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.pkg.tasks.PackageDeploy
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

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

    private fun Project.setupTasks() {
        with(AemExtension.of(this).tasks) {
            register<InstanceDown>(InstanceDown.NAME)
            register<InstanceUp>(InstanceUp.NAME) {
                dependsOn(InstanceCreate.NAME).mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME, InstanceDown.NAME)
            }
            register<InstanceRestart>(InstanceRestart.NAME) {
                dependsOn(InstanceDown.NAME, InstanceUp.NAME)
            }
            register<InstanceCreate>(InstanceCreate.NAME) {
                dependsOn(Resolve.NAME).mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
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
            register<InstanceAwait>(InstanceAwait.NAME) {
                mustRunAfter(InstanceCreate.NAME, InstanceUp.NAME, InstanceSatisfy.NAME)
                plugins.withId(PackagePlugin.ID) { mustRunAfter(PackageDeploy.NAME) }
            }
            register<InstanceCollect>(InstanceCollect.NAME) {
                mustRunAfter(InstanceSatisfy.NAME)
            }
            register<InstanceSetup>(InstanceSetup.NAME) {
                dependsOn(InstanceCreate.NAME, InstanceUp.NAME, InstanceSatisfy.NAME).mustRunAfter(InstanceDestroy.NAME)
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
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.instance"

        const val FILES_PATH = "instance"
    }
}