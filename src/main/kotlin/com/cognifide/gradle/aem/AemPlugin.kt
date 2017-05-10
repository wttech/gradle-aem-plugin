package com.cognifide.gradle.aem

import com.cognifide.gradle.aem.deploy.*
import com.cognifide.gradle.aem.jar.ProcessClassesTask
import com.cognifide.gradle.aem.jar.ProcessTestClassesTask
import com.cognifide.gradle.aem.jar.UpdateManifestTask
import com.cognifide.gradle.aem.pkg.ComposeTask
import com.cognifide.gradle.aem.vlt.CheckoutTask
import com.cognifide.gradle.aem.vlt.CleanTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * JVM based languages like Groovy or Kotlin have implicitly applied 'java' plugin. We also need 'osgi' plugin,
 * because we are updating jar manifest with OSGi specific instructions, so both plugins need to be applied.
 *
 * Projects can have only 'aem' plugin applied intentionally to generate packages with content only.
 */
class AemPlugin : Plugin<Project> {

    companion object {
        val TASK_GROUP = "AEM"

        val CONFIG_INSTALL = "aemInstall"

        val CONFIG_EMBED = "aemEmbed"

        val CONFIG_SOURCE_SETS = listOf(SourceSet.MAIN_SOURCE_SET_NAME, SourceSet.TEST_SOURCE_SET_NAME)

        val VLT_PATH = "META-INF/vault"

        val JCR_ROOT = "jcr_root"

        val OSGI_INF = "OSGI-INF"

        val OSGI_EMBED = "OSGI-INF/lib"
    }

    override fun apply(project: Project) {
        setupDependentPlugins(project)
        setupExtensions(project)
        setupTasks(project)
        setupConfigs(project)
    }

    private fun setupDependentPlugins(project: Project) {
        project.plugins.apply(BasePlugin::class.java)
    }

    private fun setupExtensions(project: Project) {
        project.extensions.create(AemExtension.NAME, AemExtension::class.java)
    }

    private fun setupTasks(project: Project) {
        val clean = project.tasks.getByName(LifecycleBasePlugin.CLEAN_TASK_NAME)

        project.plugins.withType(JavaPlugin::class.java, {
            val jar = project.tasks.getByName(JavaPlugin.JAR_TASK_NAME)
            val processClasses = project.tasks.create(ProcessClassesTask.NAME, ProcessClassesTask::class.java)
            val processTestClasses = project.tasks.create(ProcessTestClassesTask.NAME, ProcessTestClassesTask::class.java)
            val updateManifest = project.tasks.create(UpdateManifestTask.NAME, UpdateManifestTask::class.java)

            processClasses.dependsOn(project.tasks.getByName(JavaPlugin.CLASSES_TASK_NAME))
            processTestClasses.dependsOn(project.tasks.getByName(JavaPlugin.TEST_CLASSES_TASK_NAME))
            updateManifest.dependsOn(processClasses, processTestClasses)
            jar.dependsOn(processClasses, processTestClasses, updateManifest)
        })

        val compose = project.tasks.create(ComposeTask.NAME, ComposeTask::class.java)
        val upload = project.tasks.create(UploadTask.NAME, UploadTask::class.java)
        val install = project.tasks.create(InstallTask.NAME, InstallTask::class.java)
        val activate = project.tasks.create(ActivateTask.NAME, ActivateTask::class.java)
        val deploy = project.tasks.create(DeployTask.NAME, DeployTask::class.java)
        val distribute = project.tasks.create(DistributeTask.NAME, DistributeTask::class.java)
        val satisfy = project.tasks.create(SatisfyTask.NAME, SatisfyTask::class.java)

        compose.dependsOn(project.tasks.getByName(LifecycleBasePlugin.ASSEMBLE_TASK_NAME), project.tasks.getByName(LifecycleBasePlugin.CHECK_TASK_NAME))
        compose.mustRunAfter(clean)

        upload.mustRunAfter(satisfy, compose)
        install.mustRunAfter(satisfy, compose, upload)
        activate.mustRunAfter(satisfy, compose, upload, install)

        deploy.mustRunAfter(satisfy, compose)
        distribute.mustRunAfter(satisfy, compose)

        val vltClean = project.tasks.create(CleanTask.NAME, CleanTask::class.java)
        val vltCheckout = project.tasks.create(CheckoutTask.NAME, CheckoutTask::class.java)

        vltClean.mustRunAfter(clean)
        vltCheckout.mustRunAfter(clean)
    }

    private fun setupConfigs(project: Project) {
        createConfig(project, CONFIG_INSTALL, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)
        createConfig(project, CONFIG_EMBED, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)
    }

    private fun createConfig(project: Project, configName: String, configToBeExtended: String): Configuration {
        val result = project.configurations.create(configName, {
            it.isTransitive = false
        })
        forConfiguration(project, configToBeExtended, { config ->
            config.extendsFrom(result)
            appendConfigurationToCompileClasspath(project, result)
        })

        return result
    }

    private fun forConfiguration(project: Project, name: String, creator: (Configuration) -> Unit) {
        val config = project.configurations.findByName(name)
        if (config != null) {
            creator(config)
        } else {
            project.configurations.whenObjectAdded {
                if (it is Configuration) {
                    if (name == it.name) {
                        creator(config)
                    }
                }
            }
        }
    }

    private fun appendConfigurationToCompileClasspath(project: Project, config: Configuration) {
        val jar = project.convention.getPlugin(JavaPluginConvention::class.java)

        jar.sourceSets.filter { CONFIG_SOURCE_SETS.contains(it.name) }.forEach { it.compileClasspath += config }
    }

}