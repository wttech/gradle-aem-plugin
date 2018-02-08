package com.cognifide.gradle.aem.bundle

import aQute.bnd.gradle.BundleTaskConvention
import com.cognifide.gradle.aem.pkg.PackagePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Jar

class BundlePlugin : Plugin<Project> {

    companion object {
        const val CONFIG_INSTALL = "aemInstall"

        const val CONFIG_EMBED = "aemEmbed"
    }

    override fun apply(project: Project) {
        setupDependentPlugins(project)
        setupBndTool(project)
        setupTasks(project)
        setupConfigurations(project)
    }

    private fun setupDependentPlugins(project: Project) {
        project.plugins.apply(JavaPlugin::class.java)
        project.plugins.apply(PackagePlugin::class.java)
    }

    private fun setupTasks(project: Project) {
        val classes = project.tasks.getByName(JavaPlugin.CLASSES_TASK_NAME)
        val testClasses = project.tasks.getByName(JavaPlugin.TEST_CLASSES_TASK_NAME)
        val bundle = project.tasks.create(BundleTask.NAME, BundleTask::class.java)
        val jar = project.tasks.getByName(JavaPlugin.JAR_TASK_NAME)

        bundle.dependsOn(classes, testClasses)
        jar.dependsOn(bundle)
    }

    private fun setupBndTool(project: Project) {
        val jar = project.tasks.getByName(JavaPlugin.JAR_TASK_NAME) as Jar
        val convention = BundleTaskConvention(jar)

        project.convention.plugins["bundle"] = convention

        val defaultBndfile = project.file("bnd.bnd")
        if (defaultBndfile.isFile) {
            convention.setBndfile(defaultBndfile)
        }

        jar.doLast {
            convention.buildBundle()
        }
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