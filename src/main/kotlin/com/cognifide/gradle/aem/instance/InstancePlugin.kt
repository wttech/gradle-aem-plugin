package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.common.CommonPlugin
import com.cognifide.gradle.aem.instance.tasks.InstanceProvision
import com.cognifide.gradle.aem.instance.tasks.InstanceRcp
import com.cognifide.gradle.aem.instance.tasks.InstanceSatisfy
import com.cognifide.gradle.aem.instance.tasks.InstanceTail
import com.cognifide.gradle.aem.instance.tasks.*
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.pkg.tasks.PackageDeploy
import com.cognifide.gradle.common.CommonDefaultPlugin
import org.gradle.api.Project
import org.gradle.api.Task

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

        val satisfy = register<InstanceSatisfy>(InstanceSatisfy.NAME)
        val provision = register<InstanceProvision>(InstanceProvision.NAME) {
            mustRunAfter(satisfy)
        }
        register<InstanceReload>(InstanceReload.NAME) {
            mustRunAfter(satisfy)
            plugins.withId(PackagePlugin.ID) { mustRunAfter(named<Task>(PackageDeploy.NAME)) }
        }
        register<InstanceAwait>(InstanceAwait.NAME) {
            mustRunAfter(satisfy)
            plugins.withId(PackagePlugin.ID) { mustRunAfter(named<Task>(PackageDeploy.NAME)) }
        }
        register<InstanceSetup>(InstanceSetup.NAME) {
            dependsOn(satisfy, provision)
            plugins.withId(PackagePlugin.ID) { dependsOn(named<Task>(PackageDeploy.NAME)) }
        }
        register<InstanceTail>(InstanceTail.NAME)
        register<InstanceRcp>(InstanceRcp.NAME) {
            mustRunAfter(satisfy, provision)
        }
        register<InstanceGroovyEval>(InstanceGroovyEval.NAME) {
            mustRunAfter(satisfy, provision)
            plugins.withId(PackagePlugin.ID) { mustRunAfter(named<Task>(PackageDeploy.NAME)) }
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.instance"
    }
}
