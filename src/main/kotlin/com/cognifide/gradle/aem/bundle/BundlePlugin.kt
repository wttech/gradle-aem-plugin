package com.cognifide.gradle.aem.bundle

import aQute.bnd.gradle.BundleTaskConvention
import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.api.AemPlugin
import com.cognifide.gradle.aem.pkg.PackagePlugin
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.commons.lang3.reflect.FieldUtils
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import java.io.File

class BundlePlugin : AemPlugin() {

    override fun Project.configure() {
        setupDependentPlugins()
        setupJavaDefaults()
        setupJavaBndTool()
        setupTestTask()
        setupConfigurations()
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

        afterEvaluate {
            tasks.named(JavaPlugin.JAR_TASK_NAME).configure { task ->
                val jar = task as Jar

                ensureJarBaseNameIfNotCustomized(jar)
                ensureJarManifestAttributes(jar)
                ensureJarEmbedded(jar)
            }
        }
    }

    private fun Project.ensureJarEmbedded(jar: Jar) {
        val embedJars = configurations.getByName(CONFIG_EMBED).files
                .asSequence()
                .map { it.name }
                .sorted()
                .toList()

        if (embedJars.isNotEmpty()) {
            logger.info("Bundle '${jar.archiveName}' has configured embedded jars: $embedJars.")
            logger.info("For each one, ensure to have its packages exported by JAR manifest attribute 'Export-Package' or 'Private-Package' using DSL: 'aem { bundle { exportPackage('x.y.z') } }'.")
        }
    }

    /**
     * Reflection is used, because in other way, default convention will provide value.
     * It is only way to know, if base name was previously customized by build script.
     */
    private fun Project.ensureJarBaseNameIfNotCustomized(jar: Jar) {
        val baseName = FieldUtils.readField(jar, "baseName", true) as String?
        if (baseName.isNullOrBlank()) {
            val groupValue = group as String?
            if (!name.isNullOrBlank() && !groupValue.isNullOrBlank()) {
                jar.baseName = AemConfig.of(project).baseName
            }
        }
    }

    /**
     * Set (if not set) or update OSGi or AEM specific jar manifest attributes.
     */
    private fun Project.ensureJarManifestAttributes(jar: Jar) {
        val config = AemConfig.of(project)
        if (!config.bundleManifestAttributes) {
            logger.debug("Bundle manifest dynamic attributes support is disabled.")
            return
        }

        val attributes = mutableMapOf<String, Any>().apply { putAll(jar.manifest.attributes) }

        if (!attributes.contains("Bundle-Name") && !description.isNullOrBlank()) {
            attributes["Bundle-Name"] = description!!
        }

        if (!attributes.contains("Bundle-SymbolicName") && config.bundlePackage.isNotBlank()) {
            attributes["Bundle-SymbolicName"] = config.bundlePackage
        }

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

    private fun Project.setupJavaBndTool() {
        val jar = tasks.getByName(JavaPlugin.JAR_TASK_NAME) as Jar
        val bundleConvention = BundleTaskConvention(jar)

        convention.plugins[BND_CONVENTION_PLUGIN] = bundleConvention

        jar.doLast {
            try {
                val config = AemConfig.of(project)
                val instructionFile = File(config.bundleBndPath)
                if (instructionFile.isFile) {
                    bundleConvention.setBndfile(instructionFile)
                }

                val instructions = config.bundleBndInstructions
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
        plugins.withType(JavaPlugin::class.java) { _ ->
            val embedConfig = configurations.create(CONFIG_EMBED) { it.isTransitive = false }
            val installConfig = configurations.create(CONFIG_INSTALL) { it.isTransitive = false }
            val implConfig = configurations.getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)

            implConfig.extendsFrom(installConfig, embedConfig)
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.bundle"

        const val CONFIG_INSTALL = "aemInstall"

        const val CONFIG_EMBED = "aemEmbed"

        const val BND_CONVENTION_PLUGIN = "bundle"
    }

}