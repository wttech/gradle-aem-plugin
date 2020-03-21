package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.bundle.BundlePlugin
import com.cognifide.gradle.aem.bundle.tasks.BundleCompose
import com.cognifide.gradle.aem.common.CommonPlugin
import com.cognifide.gradle.aem.common.tasks.PackageTask
import com.cognifide.gradle.aem.instance.InstancePlugin
import com.cognifide.gradle.aem.instance.tasks.*
import com.cognifide.gradle.aem.pkg.tasks.*
import com.cognifide.gradle.common.CommonDefaultPlugin
import com.cognifide.gradle.common.tasks.configureApply
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

    private fun Project.setupInstallRepository() = afterEvaluate {
        val packageOptions = AemExtension.of(this).packageOptions
        if (packageOptions.installRepository.get()) {
            val installDir = file("${packageOptions.jcrRootDir.get().asFile}${packageOptions.installPath}")
            if (installDir.exists()) {
                repositories.flatDir { it.dir(installDir) }
            }
        }
    }

    @Suppress("LongMethod")
    private fun Project.setupTasks() = tasks {
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

            if (plugins.hasPlugin(BundlePlugin::class.java)) {
                val test = named<Test>(JavaPlugin.TEST_TASK_NAME)
                val bundle = named<BundleCompose>(BundleCompose.NAME)

                configureApply {
                    installBundleBuilt(bundle)
                    dependsOn(test)
                }
            }
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
        }
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
                val satisfy = named<Task>(InstanceSatisfy.NAME)
                val provision = named<Task>(InstanceProvision.NAME)
                val setup = named<Task>(InstanceSetup.NAME)

                configureApply {
                    mustRunAfter(create, up, satisfy, provision, setup)
                }
            }
        }

        register<PackageDelete>(PackageDelete.NAME) {
            dependsOn(compose, validate)
            mustRunAfter(deploy, upload, install, activate, uninstall)
        }
        register<PackagePurge>(PackagePurge.NAME) {
            dependsOn(compose, validate)
            mustRunAfter(deploy, upload, install, activate, uninstall)
        }
        named<Task>(LifecycleBasePlugin.ASSEMBLE_TASK_NAME) {
            dependsOn(compose)
        }
        named<Task>(LifecycleBasePlugin.CHECK_TASK_NAME) {
            dependsOn(validate)
        }
        typed<PackageTask> {
            files.from(compose.map { it.archiveFile })
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.package"
    }
}
