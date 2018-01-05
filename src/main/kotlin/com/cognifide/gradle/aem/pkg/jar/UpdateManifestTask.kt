package com.cognifide.gradle.aem.pkg.jar

import com.cognifide.gradle.aem.base.api.AemDefaultTask
import com.cognifide.gradle.aem.pkg.PackagePlugin
import org.dm.gradle.plugins.bundle.BundleExtension
import org.gradle.api.java.archives.Manifest
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.osgi.OsgiManifest
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
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
        val NAME = "aemUpdateManifest"

        val OSGI_PLUGIN_ID = "osgi"

        val BUNDLE_PLUGIN_ID = "org.dm.bundle"
    }

    init {
        description = "Update OSGi manifest instructions"
    }

    @Internal
    val jar = project.tasks.getByName(JavaPlugin.JAR_TASK_NAME) as Jar

    @Internal
    val test = project.tasks.getByName(JavaPlugin.TEST_TASK_NAME) as Test

    val embeddableJars: List<File>
        @InputFiles
        get() {
            return project.configurations.getByName(PackagePlugin.CONFIG_EMBED).files.sortedBy { it.name }
        }

    init {
        project.afterEvaluate {
            configureTest()
            embedJars()
        }
    }

    private fun configureTest() {
        if (config.testClasspathArchive) {
            test.classpath += project.files(jar.archivePath)
        }
    }

    private fun embedJars() {
        if (embeddableJars.isEmpty()) {
            return
        }

        project.logger.info("Embedding jar files: ${embeddableJars.map { it.name }}")

        jar.from(embeddableJars)
        addInstruction("Bundle-ClassPath", {
            val list = mutableListOf(".")
            embeddableJars.onEach { jar -> list.add(jar.name) }
            list.joinToString(",")
        })
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
        // nothing to do in execution phase right now, hook for later ;)
    }

}