package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.AemPlugin
import com.cognifide.gradle.aem.config.ConfigPlugin
import com.cognifide.gradle.aem.instance.tasks.*
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.pkg.tasks.Deploy
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
            register<Resolve>(Resolve.NAME) {
                mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
            }
            register<Down>(Down.NAME)
            register<Up>(Up.NAME) {
                dependsOn(Create.NAME).mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME, Down.NAME)
            }
            register<Restart>(Restart.NAME) {
                dependsOn(Down.NAME, Up.NAME)
            }
            register<Create>(Create.NAME) {
                dependsOn(Resolve.NAME).mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
            }
            register<Destroy>(Destroy.NAME) {
                dependsOn(Down.NAME)
            }
            register<Satisfy>(Satisfy.NAME) {
                dependsOn(Resolve.NAME).mustRunAfter(Create.NAME, Up.NAME)
            }
            register<Reload>(Reload.NAME) {
                mustRunAfter(Satisfy.NAME)
                plugins.withId(PackagePlugin.ID) { mustRunAfter(Deploy.NAME) }
            }
            register<Await>(Await.NAME) {
                mustRunAfter(Create.NAME, Up.NAME, Satisfy.NAME)
                plugins.withId(PackagePlugin.ID) { mustRunAfter(Deploy.NAME) }
            }
            register<Collect>(Collect.NAME) {
                mustRunAfter(Satisfy.NAME)
            }
            register<Setup>(Setup.NAME) {
                dependsOn(Create.NAME, Up.NAME, Satisfy.NAME).mustRunAfter(Destroy.NAME)
                plugins.withId(PackagePlugin.ID) { dependsOn(Deploy.NAME) }
            }
            register<Resetup>(Resetup.NAME) {
                dependsOn(Destroy.NAME, Setup.NAME)
            }
            register<Backup>(Backup.NAME) {
                dependsOn(Down.NAME)
                finalizedBy(Up.NAME)
            }
            register<Tail>(Tail.NAME)
            register<DockerDeploy>(DockerDeploy.NAME)
            register<DockerRm>(DockerRm.NAME)
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.instance"

        const val FILES_PATH = "local-instance"
    }
}