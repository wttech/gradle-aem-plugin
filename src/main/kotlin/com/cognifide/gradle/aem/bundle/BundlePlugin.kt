package com.cognifide.gradle.aem.bundle

import aQute.bnd.gradle.BundleTaskConvention
import com.cognifide.gradle.aem.api.AemExtension
import com.cognifide.gradle.aem.api.AemPlugin
import com.cognifide.gradle.aem.pkg.PackagePlugin
import java.io.File
import org.apache.commons.lang3.exception.ExceptionUtils
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test

class BundlePlugin : AemPlugin() {

    override fun Project.configure() {
        setupDependentPlugins()
        setupJavaDefaults()
        setupJavaBndTool()
        setupTestTask()
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

        tasks.withType(JavaCompile::class.java).configureEach {
            with(it as JavaCompile) {
                options.encoding = "UTF-8"
                options.compilerArgs = it.options.compilerArgs + "-Xlint:deprecation"
                options.isIncremental = true
            }
        }
    }

    private fun Project.setupJavaBndTool() {
        val jar = tasks.getByName(JavaPlugin.JAR_TASK_NAME) as Jar
        val bundleConvention = BundleTaskConvention(jar)

        convention.plugins[BND_CONVENTION_PLUGIN] = bundleConvention

        jar.doLast {
            try {
                val bundle = AemExtension.of(project).bundle
                val instructionFile = File(bundle.bndPath)
                if (instructionFile.isFile) {
                    bundleConvention.setBndfile(instructionFile)
                }

                val instructions = bundle.bndInstructions
                if (instructions.isNotEmpty()) {
                    bundleConvention.bnd(instructions)
                }

                bundleConvention.buildBundle()
            } catch (e: Exception) {
                logger.error("BND tool error: https://bnd.bndtools.org", ExceptionUtils.getRootCause(e))
                throw BundleException("Bundle cannot be built properly.", e)
            }
        }
    }

    /**
     * @see <https://github.com/Cognifide/gradle-aem-plugin/issues/95>
     */
    private fun Project.setupTestTask() {
        val testImplConfig = configurations.getByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME)
        val compileOnlyConfig = configurations.getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME)

        testImplConfig.extendsFrom(compileOnlyConfig)

        val test = tasks.getByName(JavaPlugin.TEST_TASK_NAME) as Test
        val jar = tasks.getByName(JavaPlugin.JAR_TASK_NAME) as Jar

        test.dependsOn(jar)
        afterEvaluate { test.classpath += files(jar.archivePath) }
    }

    companion object {
        const val ID = "com.cognifide.aem.bundle"

        const val BND_CONVENTION_PLUGIN = "bundle"
    }
}