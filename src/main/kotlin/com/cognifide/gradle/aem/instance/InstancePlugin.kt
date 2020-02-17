package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.common.CommonPlugin
import com.cognifide.gradle.aem.instance.provision.InstanceProvision
import com.cognifide.gradle.aem.instance.rcp.InstanceRcp
import com.cognifide.gradle.aem.instance.satisfy.InstanceSatisfy
import com.cognifide.gradle.aem.instance.tail.InstanceTail
import com.cognifide.gradle.aem.instance.tasks.*
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.pkg.tasks.PackageDeploy
import com.cognifide.gradle.common.CommonDefaultPlugin
import org.gradle.api.Project

class InstancePlugin : CommonDefaultPlugin() {

    override fun Project.configureProject() {
        setupDependentPlugins()
        setupTasks()
    }

    private fun Project.setupDependentPlugins() {
        plugins.apply(CommonPlugin::class.java)
    }

    private fun Project.setupTasks() = tasks {

        // Plugin tasks

        register<InstanceSatisfy>(InstanceSatisfy.NAME)
        register<InstanceProvision>(InstanceProvision.NAME) {
            mustRunAfter(InstanceSatisfy.NAME)
        }
        register<InstanceReload>(InstanceReload.NAME) {
            mustRunAfter(InstanceSatisfy.NAME)
            plugins.withId(PackagePlugin.ID) { mustRunAfter(PackageDeploy.NAME) }
        }
        register<InstanceAwait>(InstanceAwait.NAME) {
            mustRunAfter(InstanceSatisfy.NAME)
            plugins.withId(PackagePlugin.ID) { mustRunAfter(PackageDeploy.NAME) }
        }
        register<InstanceSetup>(InstanceSetup.NAME) {
            dependsOn(InstanceSatisfy.NAME, InstanceProvision.NAME)
            plugins.withId(PackagePlugin.ID) { dependsOn(PackageDeploy.NAME) }
        }
        register<InstanceTail>(InstanceTail.NAME)
        register<InstanceRcp>(InstanceRcp.NAME) {
            mustRunAfter(InstanceSatisfy.NAME, InstanceProvision.NAME)
        }
        register<InstanceGroovyEval>(InstanceGroovyEval.NAME) {
            mustRunAfter(InstanceSatisfy.NAME, InstanceProvision.NAME)
            plugins.withId(PackagePlugin.ID) { mustRunAfter(PackageDeploy.NAME) }
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.instance"
    }
}
