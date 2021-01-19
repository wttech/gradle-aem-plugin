package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.common.CommonPlugin
import com.cognifide.gradle.aem.instance.tasks.InstanceProvision
import com.cognifide.gradle.aem.instance.tasks.InstanceRcp
import com.cognifide.gradle.aem.instance.tasks.InstanceTail
import com.cognifide.gradle.aem.instance.tasks.*
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.pkg.tasks.PackageDeploy
import com.cognifide.gradle.common.CommonDefaultPlugin
import com.cognifide.gradle.common.tasks.configureApply
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

class InstancePlugin : CommonDefaultPlugin() {

    override fun Project.configureProject() {
        setupDependentPlugins()
        setupTasks()
    }

    private fun Project.setupDependentPlugins() {
        if (plugins.hasPlugin(PackagePlugin::class.java)) {
            throw AemException("Instance plugin '$ID' must be applied before package plugin '${PackagePlugin.ID}'!")
        }

        plugins.apply(CommonPlugin::class.java)
    }

    private fun Project.setupTasks() = tasks {

        val mustRunAfterPackageDeploy: TaskProvider<*>.() -> Unit = {
            if (plugins.hasPlugin(PackagePlugin::class.java)) {
                val deploy = named<Task>(PackageDeploy.NAME)
                configureApply { mustRunAfter(deploy) }
            }
        }

        val dependsOnPackageDeploy: TaskProvider<*>.() -> Unit = {
            if (plugins.hasPlugin(PackagePlugin::class.java)) {
                val deploy = named<Task>(PackageDeploy.NAME)
                configureApply { dependsOn(deploy) }
            }
        }

        // Plugin tasks

        val provision = register<InstanceProvision>(InstanceProvision.NAME)

        register<InstanceReload>(InstanceReload.NAME).apply(mustRunAfterPackageDeploy)
        register<InstanceAwait>(InstanceAwait.NAME).apply(mustRunAfterPackageDeploy)

        register<InstanceSetup>(InstanceSetup.NAME) {
            dependsOn(provision)
        }.apply(dependsOnPackageDeploy)

        register<InstanceStatus>(InstanceStatus.NAME)
        register<InstanceTail>(InstanceTail.NAME)

        register<InstanceRcp>(InstanceRcp.NAME) {
            mustRunAfter(provision)
        }

        register<InstanceDeploy>(InstanceDeploy.NAME) {
            mustRunAfter(provision)
        }

        register<InstanceGroovyEval>(InstanceGroovyEval.NAME) {
            mustRunAfter(provision)
        }.apply(mustRunAfterPackageDeploy)
    }

    companion object {
        const val ID = "com.cognifide.aem.instance"
    }
}
