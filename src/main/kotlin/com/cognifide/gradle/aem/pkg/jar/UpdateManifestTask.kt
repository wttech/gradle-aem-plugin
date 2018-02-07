package com.cognifide.gradle.aem.pkg.jar

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.pkg.PackagePlugin
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.dm.gradle.plugins.bundle.BundleExtension
import org.gradle.api.artifacts.Configuration
import org.gradle.api.java.archives.Manifest
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.osgi.OsgiManifest
import org.gradle.api.tasks.*
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import java.io.File

/**
 * Update manifest being used by 'jar' task of Java Plugin.
 * Supported OSGi related plugins are: 'osgi', 'biz.aQute.bnd.builder', 'org.dm.bundle' and others.
 *
 * @see <https://github.com/bndtools/bnd/tree/master/biz.aQute.bnd.gradle>
 * @see <https://github.com/TomDmitriev/gradle-bundle-plugin>
 */
open class UpdateManifestTask : AemDefaultTask() {

    companion object {
        const val NAME = "aemUpdateManifest"

        const val OSGI_PLUGIN_ID = "osgi"

        const val BUNDLE_PLUGIN_ID = "org.dm.bundle"
    }

    init {
        description = "Update OSGi manifest instructions"
    }

    @get:Internal
    val jar: Jar
        get() = project.tasks.getByName(JavaPlugin.JAR_TASK_NAME) as Jar

    @get:Internal
    val test: Test
        get() = project.tasks.getByName(JavaPlugin.TEST_TASK_NAME) as Test

    @get:Internal
    val embedConfig: Configuration
        get() = project.configurations.getByName(PackagePlugin.CONFIG_EMBED)

    @get:Input
    val embedDependencies
        get() = embedConfig.allDependencies.map { it.toString() }

    @get:Internal
    val embedJars: List<File>
        get() = embedConfig.files.sortedBy { it.name }

    @OutputDirectory
    val embedDownloadDir = AemTask.temporaryDir(project, NAME, "embedJars")

    @get:Internal
    val embedDownloadJars
        get() = embedDownloadDir.listFiles({ _: File, name: String? -> FilenameUtils.getExtension(name) == "jar" })

    init {
        project.afterEvaluate {
            configureTest()
            configureEmbeddingJars()
        }
    }

    private fun configureTest() {
        if (config.testClasspathJarIncluded) {
            test.classpath += project.files(jar.archivePath)
        }
    }

    private fun configureEmbeddingJars() {
        if (embedDependencies.isEmpty()) {
            return
        }

        project.logger.info("Embedding dependencies: $embedDependencies")

        jar.from(embedDownloadDir)
        jar.doFirst {
            addInstruction("Bundle-ClassPath", {
                val list = mutableListOf(".")
                embedDownloadJars.forEach { jar -> list.add(jar.name) }
                list.joinToString(",")
            })
        }
    }

    private fun addInstruction(name: String, valueProvider: () -> String) {
        if (project.plugins.hasPlugin(OSGI_PLUGIN_ID)) {
            addInstruction(jar.manifest as OsgiManifest, name, valueProvider())
        } else if (project.plugins.hasPlugin(BUNDLE_PLUGIN_ID)) {
            addInstruction(project.extensions.getByType(BundleExtension::class.java), name, valueProvider())
        } else {
            addInstruction(jar.manifest, name, valueProvider())
        }
    }

    private fun addInstruction(manifest: OsgiManifest, name: String, value: String?) {
        if (!manifest.instructions.containsKey(name)) {
            if (!value.isNullOrBlank()) {
                manifest.instruction(name, value)
            }
        }
    }

    @Suppress("unchecked_cast")
    private fun addInstruction(config: BundleExtension, name: String, value: String?) {
        val instructions = config.instructions as Map<String, Any>
        if (!instructions.contains(name)) {
            if (!value.isNullOrBlank()) {
                config.instruction(name, value)
            }
        }
    }

    private fun addInstruction(manifest: Manifest, name: String, value: String?) {
        if (!manifest.attributes.containsKey(name)) {
            if (!value.isNullOrBlank()) {
                manifest.attributes(mapOf(name to value))
            }
        }
    }

    @TaskAction
    fun updateManifest() {
        embedJars.forEach { FileUtils.copyFileToDirectory(it, embedDownloadDir) }
    }

}