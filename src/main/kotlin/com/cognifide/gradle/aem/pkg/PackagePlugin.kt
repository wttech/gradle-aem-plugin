package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.bundle.BundlePlugin
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
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Usage
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.internal.artifacts.ArtifactAttributes
import org.gradle.api.internal.artifacts.dsl.LazyPublishArtifact
import org.gradle.api.model.ObjectFactory
import org.gradle.internal.component.external.descriptor.MavenScope
import org.gradle.language.base.plugins.LifecycleBasePlugin
import javax.inject.Inject

class PackagePlugin @Inject constructor(private val objectFactory: ObjectFactory) : CommonDefaultPlugin() {

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
        val configuration = configurations.create(CONFIGURATION) { c ->
            c.attributes.attribute(Usage.USAGE_ATTRIBUTE, objectFactory.named(Usage::class.java, Usage.JAVA_RUNTIME))
            c.outgoing.attributes.attribute(ArtifactAttributes.ARTIFACT_FORMAT, ArtifactTypeDefinition.ZIP_TYPE)
        }
        configurations.named(Dependency.ARCHIVES_CONFIGURATION) { it.extendsFrom(configuration) }
        components.named(CommonPlugin.COMPONENT).configure { component ->
            if (component is AdhocComponentWithVariants) {
                component.addVariantsFromConfiguration(configuration) { it.mapToMavenScope(MavenScope.Runtime.lowerName) }
            }
        }

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
                configuration.outgoing.artifacts.add(LazyPublishArtifact(this))
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
    }

    companion object {
        const val ID = "com.cognifide.aem.package"

        const val CONFIGURATION = "aemPackage"
    }
}
