package com.cognifide.gradle.aem

import com.cognifide.gradle.aem.deploy.*
import com.cognifide.gradle.aem.pkg.ComposeTask
import com.cognifide.gradle.aem.pkg.JarEmbedder
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.osgi.OsgiPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.language.base.plugins.LifecycleBasePlugin

class AemPlugin : Plugin<Project> {

    companion object {
        val TASK_GROUP = "AEM"

        val CONFIG_PROVIDE = "aemProvide"

        val CONFIG_INSTALL = "aemInstall"

        val CONFIG_EMBED = "aemEmbed"

        val CONFIG_SOURCE_SETS = listOf(SourceSet.MAIN_SOURCE_SET_NAME, SourceSet.TEST_SOURCE_SET_NAME)

        val VLT_PATH = "META-INF/vault"

        fun globalExtension(project: Project): AemExtension {
            val container = project.rootProject.extensions
            var extension = container.findByName(AemExtension.NAME) as AemExtension?
            if (extension == null) {
                extension = container.create(AemExtension.NAME, AemExtension::class.java) as AemExtension
            }

            return extension
        }
    }

    override fun apply(project: Project) {
        setupDependentPlugins(project)
        setupExtensions(project)
        setupTasks(project)
        setupConfigs(project)
        setupHooks(project)
    }

    private fun setupDependentPlugins(project: Project) {
        project.plugins.apply(BasePlugin::class.java)
        project.plugins.apply(OsgiPlugin::class.java)
    }

    private fun setupExtensions(project: Project) {
        project.extensions.add(AemExtension.NAME, globalExtension(project))
    }

    private fun setupTasks(project: Project) {
        val clean = project.tasks.getByName(LifecycleBasePlugin.CLEAN_TASK_NAME)
        val compose = project.tasks.create(ComposeTask.NAME, ComposeTask::class.java)
        val upload = project.tasks.create(UploadTask.NAME, UploadTask::class.java)
        val install = project.tasks.create(InstallTask.NAME, InstallTask::class.java)
        val activate = project.tasks.create(ActivateTask.NAME, ActivateTask::class.java)
        val deploy = project.tasks.create(DeployTask.NAME, DeployTask::class.java)
        val distribute = project.tasks.create(DistributeTask.NAME, DistributeTask::class.java)

        compose.dependsOn(project.tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME))
        compose.mustRunAfter(clean)

        upload.mustRunAfter(compose)
        install.mustRunAfter(compose, upload)
        activate.mustRunAfter(compose, upload, install)

        deploy.dependsOn(upload, install)
        distribute.dependsOn(upload, install, activate)
    }

    private fun setupConfigs(project: Project) {
        createConfig(project, CONFIG_PROVIDE, JavaPlugin.COMPILE_CONFIGURATION_NAME, true)
        createConfig(project, CONFIG_INSTALL, JavaPlugin.COMPILE_CONFIGURATION_NAME, false)
        createConfig(project, CONFIG_EMBED, JavaPlugin.COMPILE_CONFIGURATION_NAME, false)
    }

    private fun createConfig(project: Project, configName: String, configToBeExtended: String, transitive: Boolean): Configuration {
        val result = project.configurations.create(configName, {
            it.isTransitive = transitive
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

    private fun setupHooks(project: Project) {
        project.plugins.withType(JavaPlugin::class.java) {
            project.tasks.getByName(JavaPlugin.JAR_TASK_NAME).doFirst { JarEmbedder(project).embed() }
        }
    }

    private fun appendConfigurationToCompileClasspath(project: Project, config: Configuration) {
        val jar = project.convention.getPlugin(JavaPluginConvention::class.java)

        jar.sourceSets.filter { CONFIG_SOURCE_SETS.contains(it.name) }.forEach { it.compileClasspath += config }
    }

}