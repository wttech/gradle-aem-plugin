package com.cognifide.gradle.aem.bundle

import aQute.bnd.gradle.BundleTaskConvention
import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.pkg.PackagePlugin
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test

class BundlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        with(project, {
            setupDependentPlugins()
            setupJavaDefaults()
            setupJavaBndTool()
            setupTestTask()
        })
    }

    private fun Project.setupDependentPlugins() {
        plugins.apply(JavaPlugin::class.java)
        plugins.apply(PackagePlugin::class.java)
    }

    private fun Project.setupJavaDefaults() {
        val convention = convention.getPlugin(JavaPluginConvention::class.java)
        convention.sourceCompatibility = JavaVersion.VERSION_1_8
        convention.targetCompatibility = JavaVersion.VERSION_1_8

        tasks.withType(JavaCompile::class.java, {
            it.options.encoding = "UTF-8"
            it.options.compilerArgs = it.options.compilerArgs + "-Xlint:deprecation"
            it.options.isIncremental = true
        })

        val jar = tasks.getByName(JavaPlugin.JAR_TASK_NAME) as Jar
        jar.baseName = "$group.$name"

        gradle.projectsEvaluated {
            if (!description.isNullOrBlank()) {
                defaultJarManifestAttribute(jar, "Bundle-Name", this.description!!)
            }

            val config = AemConfig.of(project)
            if (config.bundlePackage.isNotBlank()) {
                defaultJarManifestAttribute(jar, "Bundle-SymbolicName", config.bundlePackage)
                defaultJarManifestAttribute(jar, "Sling-Model-Packages", config.bundlePackage)

                val exportPackage = (listOf(config.bundlePackage) + config.bundleEmbedExport).joinToString(",") {
                    packageWithBundleEmbedOptions(config, it)
                }
                defaultJarManifestAttribute(jar, "Export-Package", exportPackage)
            }

            if (config.bundleEmbedPrivate.isNotEmpty()) {
                val privatePackage = config.bundleEmbedPrivate.joinToString(",") {
                    packageWithBundleEmbedOptions(config, it)
                }
                defaultJarManifestAttribute(jar, "Private-Package", privatePackage)
            }
        }
    }

    private fun packageWithBundleEmbedOptions(config: AemConfig, pkg: String): String {
        return if (config.bundlePackageOptions.isNotBlank()) {
            "$pkg.*;${config.bundlePackageOptions}"
        } else {
            "$pkg.*"
        }
    }

    private fun defaultJarManifestAttribute(jar: Jar, key: String, value: String) {
        if (jar.manifest.attributes[key] == null) {
            jar.manifest.attributes(mapOf(key to value))
        }
    }

    private fun Project.setupJavaBndTool() {
        val jar = tasks.getByName(JavaPlugin.JAR_TASK_NAME) as Jar
        val bundleConvention = BundleTaskConvention(jar)

        convention.plugins[BND_CONVENTION_PLUGIN] = bundleConvention

        val bndFile = file(BND_FILE)
        if (bndFile.isFile) {
            bundleConvention.setBndfile(bndFile)
        }

        jar.doLast {
            bundleConvention.buildBundle()
        }
    }

    /**
     * @see <https://github.com/Cognifide/gradle-aem-plugin/issues/95>
     */
    private fun Project.setupTestTask() {
        val testSourceSet = convention.getPlugin(JavaPluginConvention::class.java)
                .sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME)
        val compileOnlyConfig = configurations.getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME)

        testSourceSet.compileClasspath += compileOnlyConfig
        testSourceSet.runtimeClasspath += compileOnlyConfig

        val test = tasks.getByName(JavaPlugin.TEST_TASK_NAME) as Test
        val jar = tasks.getByName(JavaPlugin.JAR_TASK_NAME) as Jar

        gradle.projectsEvaluated { test.classpath += files(jar.archivePath) }
    }

    companion object {
        const val BND_FILE = "bnd.bnd"

        const val BND_CONVENTION_PLUGIN = "bundle"
    }

}