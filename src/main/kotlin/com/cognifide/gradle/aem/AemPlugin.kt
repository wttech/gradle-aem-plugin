package com.cognifide.gradle.aem

import com.cognifide.gradle.aem.deploy.ActivateTask
import com.cognifide.gradle.aem.deploy.InstallTask
import com.cognifide.gradle.aem.deploy.UploadTask
import com.cognifide.gradle.aem.pkg.bundle.JarEmbedder
import com.cognifide.gradle.aem.pkg.task.AssemblePackage
import com.cognifide.gradle.aem.pkg.task.CreatePackage
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention

class AemPlugin : Plugin<Project> {

    companion object {
        val TASK_GROUP = "AEM"

        val CONFIG_EXTENSION = "aem"

        val CONFIG_PROVIDE = "aemProvide"

        val CONFIG_INSTALL = "aemInstall"

        val CONFIG_EMBED = "aemEmbed"

        val CONFIG_SOURCE_SETS = listOf("main", "test")

        val VLT_PATH = "META-INF/vault"
    }

    override fun apply(project: Project) {
        project.plugins.apply(BasePlugin::class.java)

        project.extensions.create(CONFIG_EXTENSION, AemConfig::class.java)

        project.tasks.create(CreatePackage.NAME, CreatePackage::class.java)
        project.tasks.create(AssemblePackage.NAME, AssemblePackage::class.java)
        project.tasks.create(UploadTask.NAME, UploadTask::class.java)
        project.tasks.create(InstallTask.NAME, InstallTask::class.java)
        project.tasks.create(ActivateTask.NAME, ActivateTask::class.java)

        setupConfigs(project)
        setupJarEmbedder(project)

        // TODO automatically define order for clean,[aemCreatePackage,aemCreateAssembly],aemUpload,aemInstall,aemActivate
        // TODO build.dependsOn aemCreatePackage ? (or user defined).. rather yes
        /**
        aemUpload + aemInstall = aemDeploy
        aemUpload + aemInstall + aemActivate = aemDistribute
         */
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

    private fun setupJarEmbedder(project: Project) {
        project.plugins.withType(JavaPlugin::class.java) {
            project.tasks.getByName(JavaPlugin.JAR_TASK_NAME).doFirst { JarEmbedder(project).embed() }
        }
    }

    private fun appendConfigurationToCompileClasspath(project: Project, config: Configuration) {
        val jar = project.convention.getPlugin(JavaPluginConvention::class.java)

        jar.sourceSets.filter { CONFIG_SOURCE_SETS.contains(it.name) }.forEach { it.compileClasspath += config }
    }

}