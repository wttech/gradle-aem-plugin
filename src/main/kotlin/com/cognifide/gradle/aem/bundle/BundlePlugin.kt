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
            setupConfigurations()
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

        afterEvaluate {
            val config = AemConfig.of(project)
            if (!config.bundleManifestAttributes) {
                logger.debug("Bundle manifest dynamic attributes support is disabled.")
                return@afterEvaluate
            }

            val attributes = mutableMapOf<String, Any>().apply { putAll(jar.manifest.attributes) }

            if (!attributes.contains("Bundle-Name") && !description.isNullOrBlank()) {
                attributes["Bundle-Name"] = description!!
            }

            if (!attributes.contains("Bundle-SymbolicName") && config.bundlePackage.isNotBlank()) {
                attributes["Bundle-SymbolicName"] = config.bundlePackage
            }

            attributes["Bundle-ClassPath"] = mutableSetOf<String>().apply {
                add(".")
                addAll(configurations.getByName(BundlePlugin.CONFIG_EMBED).files.sortedBy { it.name }.map { it.name })
                addAll((attributes["Bundle-ClassPath"]?.toString() ?: "").split(",").map { it.trim() })
            }.joinToString(",")

            attributes["Export-Package"] = mutableSetOf<String>().apply {
                if (config.bundlePackage.isNotBlank()) {
                    add(if (config.bundlePackageOptions.isNotBlank()) {
                        "${config.bundlePackage}.*;${config.bundlePackageOptions}"
                    } else {
                        "${config.bundlePackage}.*"
                    })
                }

                addAll((attributes["Export-Package"]?.toString() ?: "").split(",").map { it.trim() })
            }.joinToString(",")

            if (!attributes.contains("Sling-Model-Packages") && config.bundlePackage.isNotBlank()) {
                attributes["Sling-Model-Packages"] = config.bundlePackage
            }

            jar.manifest.attributes(attributes)
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

    private fun Project.setupConfigurations() {
        plugins.withType(JavaPlugin::class.java, {
            val embedConfig = configurations.create(CONFIG_EMBED, { it.isTransitive = false })
            val installConfig = configurations.create(CONFIG_INSTALL, { it.isTransitive = false })
            val implConfig = configurations.getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)

            implConfig.extendsFrom(installConfig, embedConfig)
        })
    }

    companion object {
        const val CONFIG_INSTALL = "aemInstall"

        const val CONFIG_EMBED = "aemEmbed"

        const val BND_FILE = "bnd.bnd"

        const val BND_CONVENTION_PLUGIN = "bundle"
    }

}