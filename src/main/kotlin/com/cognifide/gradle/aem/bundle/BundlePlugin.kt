package com.cognifide.gradle.aem.bundle

import aQute.bnd.gradle.BundleTaskConvention
import com.cognifide.gradle.aem.pkg.PackagePlugin
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test

class BundlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        setupDependentPlugins(project)
        setupJavaDefaults(project)
        setupJavaBndTool(project)
        setupBundleTask(project)
        setupTestTask(project)
        setupConfigurations(project)
    }

    private fun setupDependentPlugins(project: Project) {
        project.plugins.apply(JavaPlugin::class.java)
        project.plugins.apply(PackagePlugin::class.java)
    }

    private fun setupJavaDefaults(project: Project) {
        val convention = project.convention.getPlugin(JavaPluginConvention::class.java)
        convention.sourceCompatibility = JavaVersion.VERSION_1_8
        convention.targetCompatibility = JavaVersion.VERSION_1_8

        project.tasks.withType(JavaCompile::class.java, {
            it.options.encoding = "UTF-8"
            it.options.compilerArgs = it.options.compilerArgs + "-Xlint:deprecation"
            it.options.isIncremental = true
        })
    }

    /**
     * @since 3.0.0
     */
    private fun setupJavaBndTool(project: Project) {
        val jar = project.tasks.getByName(JavaPlugin.JAR_TASK_NAME) as Jar
        val convention = BundleTaskConvention(jar)

        project.convention.plugins[BND_CONVENTION_PLUGIN] = convention

        val bndFile = project.file(BND_FILE)
        if (bndFile.isFile) {
            convention.setBndfile(bndFile)
        }

        jar.doLast {
            convention.buildBundle()
        }
    }

    private fun setupBundleTask(project: Project) {
        val classes = project.tasks.getByName(JavaPlugin.CLASSES_TASK_NAME)
        val testClasses = project.tasks.getByName(JavaPlugin.TEST_CLASSES_TASK_NAME)
        val bundle = project.tasks.create(BundleTask.NAME, BundleTask::class.java)
        val jar = project.tasks.getByName(JavaPlugin.JAR_TASK_NAME)

        bundle.dependsOn(classes, testClasses)
        jar.dependsOn(bundle)
    }

    /**
     * @see <https://github.com/Cognifide/gradle-aem-plugin/issues/95>
     */
    private fun setupTestTask(project: Project) {
        val testSourceSet = project.convention.getPlugin(JavaPluginConvention::class.java)
                .sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME)
        val compileOnlyConfig = project.configurations.getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME)

        testSourceSet.compileClasspath += compileOnlyConfig
        testSourceSet.runtimeClasspath += compileOnlyConfig

        val test = project.tasks.getByName(JavaPlugin.TEST_TASK_NAME) as Test
        val jar = project.tasks.getByName(JavaPlugin.JAR_TASK_NAME) as Jar

        project.gradle.projectsEvaluated { test.classpath += project.files(jar.archivePath) }
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

    companion object {
        const val CONFIG_INSTALL = "aemInstall"

        const val CONFIG_EMBED = "aemEmbed"

        const val BND_FILE = "bnd.bnd"

        const val BND_CONVENTION_PLUGIN = "bundle"
    }

}