package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.instance.provision.InstanceProvision
import com.cognifide.gradle.aem.instance.rcp.InstanceRcp
import com.cognifide.gradle.aem.instance.satisfy.InstanceSatisfy
import com.cognifide.gradle.aem.instance.tail.InstanceTail
import com.cognifide.gradle.aem.instance.tasks.*
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.pkg.tasks.PackageDeploy
import com.cognifide.gradle.common.CommonDefaultPlugin
import com.cognifide.gradle.common.RuntimePlugin
import com.cognifide.gradle.common.tasks.runtime.*
import org.gradle.api.Project

/**
 * Separate plugin which provides tasks for:
 * - managing local instances (create, up, down)
 * - monitoring health condition (check)
 * - automatically installing dependent CRX packages (satisfy)
 *
 * Most often should be applied only to one project in build (typically project named 'aem' or root project).
 * Applying it multiple times to same configuration could case confusing errors like AEM started multiple times.
 */
class InstancePlugin : CommonDefaultPlugin() {

    override fun Project.configureProject() {
        setupDependentPlugins()
        setupTasks()
    }

    private fun Project.setupDependentPlugins() {
        plugins.apply(RuntimePlugin::class.java)
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
                mustRunAfter(InstanceDestroy.NAME, InstanceResolve.NAME)
            }
            register<InstanceDestroy>(InstanceDestroy.NAME) {
                dependsOn(InstanceDown.NAME)
            }
            register<InstanceSatisfy>(InstanceSatisfy.NAME) {
                mustRunAfter(InstanceResolve.NAME, InstanceCreate.NAME, InstanceUp.NAME)
            }
            register<InstanceProvision>(InstanceProvision.NAME) {
                mustRunAfter(InstanceResolve.NAME, InstanceCreate.NAME, InstanceUp.NAME, InstanceSatisfy.NAME)
            }
            register<InstanceReload>(InstanceReload.NAME) {
                mustRunAfter(InstanceSatisfy.NAME)
                plugins.withId(PackagePlugin.ID) { mustRunAfter(PackageDeploy.NAME) }
            }
            register<InstanceAwait>(InstanceAwait.NAME) {
                mustRunAfter(InstanceCreate.NAME, InstanceUp.NAME, InstanceSatisfy.NAME)
                plugins.withId(PackagePlugin.ID) { mustRunAfter(PackageDeploy.NAME) }
            }
            register<InstanceSetup>(InstanceSetup.NAME) {
                dependsOn(InstanceCreate.NAME, InstanceUp.NAME, InstanceSatisfy.NAME, InstanceProvision.NAME)
                mustRunAfter(InstanceDestroy.NAME)
                plugins.withId(PackagePlugin.ID) { dependsOn(PackageDeploy.NAME) }
            }
            register<InstanceResetup>(InstanceResetup.NAME) {
                dependsOn(InstanceDestroy.NAME, InstanceSetup.NAME)
            }
            register<InstanceBackup>(InstanceBackup.NAME) {
                mustRunAfter(InstanceDown.NAME)
            }
            register<InstanceResolve>(InstanceResolve.NAME)
            register<InstanceTail>(InstanceTail.NAME) {
                mustRunAfter(InstanceResolve.NAME, InstanceCreate.NAME, InstanceUp.NAME)
            }
            register<InstanceRcp>(InstanceRcp.NAME) {
                mustRunAfter(InstanceResolve.NAME, InstanceCreate.NAME, InstanceUp.NAME)
            }
            register<InstanceGroovyEval>(InstanceGroovyEval.NAME) {
                mustRunAfter(InstanceResolve.NAME, InstanceCreate.NAME, InstanceUp.NAME, InstanceSatisfy.NAME, InstanceProvision.NAME)
                plugins.withId(PackagePlugin.ID) { mustRunAfter(PackageDeploy.NAME) }
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
    }

    companion object {
        const val ID = "com.cognifide.aem.instance"
    }
}
