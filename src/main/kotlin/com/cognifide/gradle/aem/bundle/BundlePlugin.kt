package com.cognifide.gradle.aem.bundle

import com.cognifide.gradle.aem.bundle.tasks.Bundle
import com.cognifide.gradle.aem.common.AemPlugin
import com.cognifide.gradle.aem.common.TaskFacade
import com.cognifide.gradle.aem.pkg.PackagePlugin
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test

class BundlePlugin : AemPlugin() {

    override fun Project.configure() {
        setupDependentPlugins()
        setupJavaDefaults()
        setupTasks()
        // TODO setupTestTask()
    }

    private fun Project.setupDependentPlugins() {
        plugins.apply(JavaPlugin::class.java)
        plugins.apply(PackagePlugin::class.java)
    }

    private fun Project.setupJavaDefaults() {
        with(convention.getPlugin(JavaPluginConvention::class.java)) {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        tasks.withType(JavaCompile::class.java).configureEach { compile ->
            with(compile as JavaCompile) {
                options.encoding = "UTF-8"
                options.compilerArgs = compile.options.compilerArgs + "-Xlint:deprecation"
                options.isIncremental = true
            }
        }
    }

    private fun Project.setupTasks() {

        with(TaskFacade(project)) {
            named(JavaPlugin.JAR_TASK_NAME, Jar::class.java) { enabled = false }
            register(Bundle.NAME, Bundle::class.java) { bundle ->
                val convention = project.convention.getPlugin(JavaPluginConvention::class.java)
                val mainSourceSet = convention.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)

                bundle.from(mainSourceSet.output)
            }
        }
    }

    /**
     * @see <https://github.com/Cognifide/gradle-aem-plugin/issues/95>
     */
    private fun Project.setupTestTask() {
        project.tasks.withType(Test::class.java).named(JavaPlugin.TEST_TASK_NAME).configure { test ->
            val testImplConfig = project.configurations.getByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME)
            val compileOnlyConfig = project.configurations.getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME)

            testImplConfig.extendsFrom(compileOnlyConfig)

            project.tasks.withType(Bundle::class.java) { bundle ->
                test.dependsOn(bundle)
                test.classpath += project.files(bundle.archivePath)
            }
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.bundle"
    }
}