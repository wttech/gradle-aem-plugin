package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.CommonPlugin
import com.cognifide.gradle.aem.common.tasks.Package
import com.cognifide.gradle.aem.instance.InstancePlugin
import com.cognifide.gradle.aem.instance.tasks.InstanceCreate
import com.cognifide.gradle.aem.instance.tasks.InstanceProvision
import com.cognifide.gradle.aem.instance.tasks.InstanceSetup
import com.cognifide.gradle.aem.instance.tasks.InstanceUp
import com.cognifide.gradle.aem.pkg.tasks.PackageActivate
import com.cognifide.gradle.aem.pkg.tasks.PackageCompose
import com.cognifide.gradle.aem.pkg.tasks.PackageDelete
import com.cognifide.gradle.aem.pkg.tasks.PackageDeploy
import com.cognifide.gradle.aem.pkg.tasks.PackageInstall
import com.cognifide.gradle.aem.pkg.tasks.PackagePrepare
import com.cognifide.gradle.aem.pkg.tasks.PackagePurge
import com.cognifide.gradle.aem.pkg.tasks.PackageUninstall
import com.cognifide.gradle.aem.pkg.tasks.PackageUpload
import com.cognifide.gradle.aem.pkg.tasks.PackageValidate
import com.cognifide.gradle.common.CommonDefaultPlugin
import com.cognifide.gradle.common.checkForce
import com.cognifide.gradle.common.tasks.configureApply
import org.gradle.api.Project
import org.gradle.api.Task
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

    private fun Project.setupInstallRepository() = afterEvaluate {
        val packageOptions = AemExtension.of(this).packageOptions
        if (packageOptions.installRepository.get()) {
            if (packageOptions.installDir.get().asFile.exists()) {
                repositories.flatDir { it.dir(packageOptions.installDir.get().asFile) }
            }
        }
    }

    @Suppress("LongMethod")
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
            }
            val validate = register<PackageValidate>(PackageValidate.NAME) {
                dependsOn(compose)
                packages.from(compose.flatMap { it.archiveFile })
            }
            val upload = register<PackageUpload>(PackageUpload.NAME) {
                dependsOn(compose, validate)
            }
            val install = register<PackageInstall>(PackageInstall.NAME) {
                dependsOn(compose, validate)
                mustRunAfter(upload)
            }
            val uninstall = register<PackageUninstall>(PackageUninstall.NAME) {
                dependsOn(compose, validate)
                mustRunAfter(upload, install)
            }.also { checkForce(it) }
            val activate = register<PackageActivate>(PackageActivate.NAME) {
                dependsOn(compose, validate)
                mustRunAfter(upload, install)
            }
            val deploy = register<PackageDeploy>(PackageDeploy.NAME) {
                dependsOn(compose, validate)
            }.apply {
                if (plugins.hasPlugin(InstancePlugin::class.java)) {
                    val create = named<Task>(InstanceCreate.NAME)
                    val up = named<Task>(InstanceUp.NAME)
                    val provision = named<Task>(InstanceProvision.NAME)
                    val setup = named<Task>(InstanceSetup.NAME)

                    configureApply {
                        mustRunAfter(create, up, provision, setup)
                    }
                }
            }

            register<PackageDelete>(PackageDelete.NAME) {
                dependsOn(compose, validate)
                mustRunAfter(deploy, upload, install, activate, uninstall)
            }.also { checkForce(it) }
            register<PackagePurge>(PackagePurge.NAME) {
                dependsOn(compose, validate)
                mustRunAfter(deploy, upload, install, activate, uninstall)
            }.also { checkForce(it) }
            named<Task>(LifecycleBasePlugin.ASSEMBLE_TASK_NAME) {
                dependsOn(compose)
            }
            named<Task>(LifecycleBasePlugin.CHECK_TASK_NAME) {
                dependsOn(validate)
            }
            typed<Package> {
                files(compose.map { it.archiveFile })
            }
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.package"
    }
}
