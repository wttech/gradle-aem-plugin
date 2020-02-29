package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.bundle.BundlePlugin
import com.cognifide.gradle.aem.bundle.tasks.BundleCompose
import com.cognifide.gradle.aem.common.CommonPlugin
import com.cognifide.gradle.aem.common.tasks.PackageTask
import com.cognifide.gradle.aem.instance.InstancePlugin
import com.cognifide.gradle.aem.instance.tasks.InstanceSatisfy
import com.cognifide.gradle.aem.instance.tasks.InstanceCreate
import com.cognifide.gradle.aem.instance.tasks.InstanceUp
import com.cognifide.gradle.aem.pkg.tasks.*
import com.cognifide.gradle.common.CommonDefaultPlugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test
import org.gradle.language.base.plugins.LifecycleBasePlugin

class PackagePlugin : CommonDefaultPlugin() {

    override fun Project.configureProject() {
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
            if (packageOptions.installRepository.get()) {
                val installDir = file("${packageOptions.jcrRootDir}${packageOptions.installPath}")
                if (installDir.exists()) {
                    repositories.flatDir { it.dir(installDir) }
                }
            }
        }
    }

    private fun Project.setupTasks() {
        tasks {
            val clean = named<Task>(LifecycleBasePlugin.CLEAN_TASK_NAME)
            val prepare = register<PackagePrepare>(PackagePrepare.NAME) {
                mustRunAfter(clean)
            }
            val compose = register<PackageCompose>(PackageCompose.NAME) {
                dependsOn(prepare)
                mustRunAfter(clean)
                metaDir.convention(prepare.flatMap { it.metaDir })
            }.apply {
                artifacts.add(Dependency.ARCHIVES_CONFIGURATION, this)

                plugins.withId(BundlePlugin.ID) {
                    val test = named<Test>(JavaPlugin.TEST_TASK_NAME)
                    val bundle = named<BundleCompose>(BundleCompose.NAME)

                    configure { task ->
                        task.installBundleBuilt(bundle)
                        task.dependsOn(test)
                    }
                }
            }
            val upload = register<PackageUpload>(PackageUpload.NAME) {
                dependsOn(compose)
            }
            val install = register<PackageInstall>(PackageInstall.NAME) {
                dependsOn(compose)
                mustRunAfter(upload)
            }
            val uninstall = register<PackageUninstall>(PackageUninstall.NAME) {
                dependsOn(compose)
                mustRunAfter(upload, install)
            }
            val activate = register<PackageActivate>(PackageActivate.NAME) {
                dependsOn(compose)
                mustRunAfter(upload, install)
            }
            val deploy = register<PackageDeploy>(PackageDeploy.NAME) {
                dependsOn(compose)
                plugins.withId(InstancePlugin.ID) {
                    mustRunAfter(named<Task>(InstanceCreate.NAME), named<Task>(InstanceUp.NAME), named<Task>(InstanceSatisfy.NAME))
                }
            }
            register<PackageDelete>(PackageDelete.NAME) {
                dependsOn(compose)
                mustRunAfter(deploy, upload, install, activate, uninstall)
            }
            register<PackagePurge>(PackagePurge.NAME) {
                dependsOn(compose)
                mustRunAfter(deploy, upload, install, activate, uninstall)
            }
            named<Task>(LifecycleBasePlugin.ASSEMBLE_TASK_NAME) {
                dependsOn(compose)
            }
            typed<PackageTask> {
                files.from(compose.map { it.archiveFile })
            }
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.package"
    }
}
