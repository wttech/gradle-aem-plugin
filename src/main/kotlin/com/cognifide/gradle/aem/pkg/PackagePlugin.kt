package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.AemPlugin
import com.cognifide.gradle.aem.config.ConfigPlugin
import com.cognifide.gradle.aem.instance.InstancePlugin
import com.cognifide.gradle.aem.instance.tasks.InstanceCreate
import com.cognifide.gradle.aem.instance.tasks.InstanceSatisfy
import com.cognifide.gradle.aem.instance.tasks.InstanceUp
import com.cognifide.gradle.aem.pkg.tasks.*
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

class PackagePlugin : AemPlugin() {

    override fun Project.configure() {
        setupDependentPlugins()
        setupInstallRepository()
        setupTasks()
    }

    private fun Project.setupDependentPlugins() {
        plugins.apply(ConfigPlugin::class.java)
    }

    private fun Project.setupInstallRepository() {
        afterEvaluate {
            val config = AemExtension.of(this).config
            if (config.packageInstallRepository) {
                val installDir = file("${config.packageJcrRoot}${config.packageInstallPath}")
                if (installDir.exists()) {
                    repositories.flatDir { it.dir(installDir) }
                }
            }
        }
    }

    private fun Project.setupTasks() {
        tasks {
            register<PackageCompose>(PackageCompose.NAME) {
                dependsOn(LifecycleBasePlugin.ASSEMBLE_TASK_NAME, LifecycleBasePlugin.CHECK_TASK_NAME)
                mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
            }
            register<PackageUpload>(PackageUpload.NAME) {
                dependsOn(PackageCompose.NAME)
            }
            register<PackageInstall>(PackageInstall.NAME) {
                dependsOn(PackageCompose.NAME)
                mustRunAfter(PackageUpload.NAME)
            }
            register<PackageUninstall>(PackageUninstall.NAME) {
                dependsOn(PackageCompose.NAME)
                mustRunAfter(PackageUpload.NAME, PackageInstall.NAME)
            }
            register<PackageActivate>(PackageActivate.NAME) {
                dependsOn(PackageCompose.NAME)
                mustRunAfter(PackageUpload.NAME, PackageInstall.NAME)
            }
            register<PackageDeploy>(PackageDeploy.NAME) {
                dependsOn(PackageCompose.NAME)
            }
            register<PackageDelete>(PackageDelete.NAME) {
                dependsOn(PackageCompose.NAME)
                mustRunAfter(PackageDeploy.NAME, PackageUpload.NAME, PackageInstall.NAME, PackageActivate.NAME, PackageUninstall.NAME)
            }
            register<PackagePurge>(PackagePurge.NAME) {
                dependsOn(PackageCompose.NAME)
                mustRunAfter(PackageDeploy.NAME, PackageUpload.NAME, PackageInstall.NAME, PackageActivate.NAME, PackageUninstall.NAME)
            }
        }

        tasks.named(LifecycleBasePlugin.BUILD_TASK_NAME).configure { it.dependsOn(PackageCompose.NAME) }

        plugins.withId(InstancePlugin.ID) {
            tasks.named(PackageDeploy.NAME).configure { task ->
                task.mustRunAfter(InstanceCreate.NAME, InstanceUp.NAME, InstanceSatisfy.NAME)
            }
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.package"
    }
}