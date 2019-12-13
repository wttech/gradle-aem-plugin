package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.common.CommonPlugin
import com.cognifide.gradle.aem.instance.InstancePlugin
import com.cognifide.gradle.aem.instance.satisfy.InstanceSatisfy
import com.cognifide.gradle.aem.instance.tasks.InstanceCreate
import com.cognifide.gradle.aem.instance.tasks.InstanceUp
import com.cognifide.gradle.aem.pkg.tasks.*
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.language.base.plugins.LifecycleBasePlugin

class PackagePlugin : AemPlugin() {

    override fun Project.configure() {
        setupDependentPlugins()
        setupInstallRepository()
        setupTasks()
    }

    private fun Project.setupDependentPlugins() {
        plugins.apply(CommonPlugin::class.java)
    }

    private fun Project.setupInstallRepository() {
        afterEvaluate {
            val packageOptions = AemExtension.of(this).packageOptions
            if (packageOptions.installRepository) {
                val installDir = file("${packageOptions.jcrRootDir}${packageOptions.installPath}")
                if (installDir.exists()) {
                    repositories.flatDir { it.dir(installDir) }
                }
            }
        }
    }

    private fun Project.setupTasks() {
        tasks {
            register<PackagePrepare>(PackagePrepare.NAME) {
                mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
            }
            register<PackageCompose>(PackageCompose.NAME) {
                dependsOn(PackagePrepare.NAME)
                mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
                metaDir = get<PackagePrepare>(PackagePrepare.NAME).metaDir
            }.apply {
                artifacts.add(Dependency.ARCHIVES_CONFIGURATION, this)
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
            named<Task>(LifecycleBasePlugin.ASSEMBLE_TASK_NAME) {
                dependsOn(PackageCompose.NAME)
            }
        }

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
