package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.base.BasePlugin
import com.cognifide.gradle.aem.pkg.deploy.*
import com.cognifide.gradle.aem.pkg.jar.UpdateManifestTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * Plugin assumptions:
 *
 * JVM based languages like Groovy or Kotlin must have implicitly applied 'java' plugin.
 * Projects can have only 'com.cognifide.aem.package' plugin applied intentionally to generate packages with content only.
 * Projects can have applied official almost any type of OSGi plugin to customize manifest ('osgi', 'biz.aQute.bnd.builder', 'org.dm.bundle').
 */
class PackagePlugin : Plugin<Project> {

    companion object {
        val ID = "com.cognifide.aem.package"

        val PKG = "com.cognifide.gradle.aem"

        val CONFIG_INSTALL = "aemInstall"

        val CONFIG_EMBED = "aemEmbed"

        val VLT_PATH = "META-INF/vault"

        val VLT_PROPERTIES = "$VLT_PATH/properties.xml"

        val JCR_ROOT = "jcr_root"

        val JAR_MANIFEST = "META-INF/MANIFEST.MF"

        val BUILD_TASK_RULE = "Pattern: aem<ProjectPath>Build: Build CRX package and deploy it to AEM instance(s)."
    }

    override fun apply(project: Project) {
        setupDependentPlugins(project)
        setupTasks(project)
        setupConfigurations(project)
    }

    private fun setupDependentPlugins(project: Project) {
        project.plugins.apply(BasePlugin::class.java)
    }

    private fun setupTasks(project: Project) {
        val clean = project.tasks.getByName(LifecycleBasePlugin.CLEAN_TASK_NAME)

        project.plugins.withType(JavaPlugin::class.java, {
            val classes = project.tasks.getByName(JavaPlugin.CLASSES_TASK_NAME)
            val testClasses = project.tasks.getByName(JavaPlugin.TEST_CLASSES_TASK_NAME)
            val updateManifest = project.tasks.create(UpdateManifestTask.NAME, UpdateManifestTask::class.java)
            val jar = project.tasks.getByName(JavaPlugin.JAR_TASK_NAME)

            updateManifest.dependsOn(classes, testClasses)
            jar.dependsOn(updateManifest)
        })

        val prepare = project.tasks.create(PrepareTask.NAME, PrepareTask::class.java)
        val compose = project.tasks.create(ComposeTask.NAME, ComposeTask::class.java)
        val upload = project.tasks.create(UploadTask.NAME, UploadTask::class.java)
        project.tasks.create(DeleteTask.NAME, DeleteTask::class.java)
        project.tasks.create(PurgeTask.NAME, PurgeTask::class.java)
        val install = project.tasks.create(InstallTask.NAME, InstallTask::class.java)
        project.tasks.create(UninstallTask.NAME, UninstallTask::class.java)
        val activate = project.tasks.create(ActivateTask.NAME, ActivateTask::class.java)
        val deploy = project.tasks.create(DeployTask.NAME, DeployTask::class.java)
        val distribute = project.tasks.create(DistributeTask.NAME, DistributeTask::class.java)

        val assemble = project.tasks.getByName(LifecycleBasePlugin.ASSEMBLE_TASK_NAME)
        val check = project.tasks.getByName(LifecycleBasePlugin.CHECK_TASK_NAME)
        val build = project.tasks.getByName(LifecycleBasePlugin.BUILD_TASK_NAME)

        assemble.mustRunAfter(clean)
        check.mustRunAfter(clean)
        build.dependsOn(compose)

        prepare.mustRunAfter(clean)

        compose.dependsOn(prepare, assemble, check)
        compose.mustRunAfter(clean)

        upload.dependsOn(compose)
        install.mustRunAfter(compose, upload)
        activate.mustRunAfter(compose, upload, install)

        deploy.dependsOn(compose)
        distribute.dependsOn(compose)
    }

    private fun setupConfigurations(project: Project) {
        project.plugins.withType(JavaPlugin::class.java, {
            val baseConfig = project.configurations.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME)
            val configurer: (Configuration) -> Unit = {
                it.isTransitive = false
                baseConfig.extendsFrom(it)
            }

            project.configurations.create(CONFIG_EMBED, configurer)
            project.configurations.create(CONFIG_INSTALL, configurer)
        })
    }

}