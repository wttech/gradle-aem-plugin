package com.cognifide.gradle.aem.bundle

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.api.AemTask
import org.apache.commons.io.FileUtils
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar
import org.gradle.util.GFileUtils
import java.io.File
import java.io.FileFilter

open class BundleTask : AemDefaultTask() {

    companion object {
        const val NAME = "aemBundle"
    }

    init {
        description = "Prepare JAR before running BND tool"
    }

    @get:Internal
    val jar: Jar
        get() = project.tasks.getByName(JavaPlugin.JAR_TASK_NAME) as Jar

    @get:Internal
    val embedConfig: Configuration
        get() = project.configurations.getByName(BundlePlugin.CONFIG_EMBED)

    @get:Input
    val embedDependencies
        get() = embedConfig.allDependencies.map { it.toString() }

    @get:Internal
    val embedJars: List<File>
        get() = embedConfig.files.sortedBy { it.name }

    @OutputDirectory
    val embedJarsDir = AemTask.temporaryDir(project, NAME, "embedJars")

    init {
        project.afterEvaluate { configureEmbedJars() }
    }

    private fun configureEmbedJars() {
        if (embedDependencies.isEmpty()) {
            return
        }

        project.logger.info("Embedding dependencies: $embedDependencies")

        jar.from(embedJarsDir)
        jar.doFirst {
            val list = mutableListOf(".").apply {
                val jars = embedJarsDir.listFiles(FileFilter { it.extension == "jar" })
                        ?: arrayOf()
                jars.sortedBy { it.name }.forEach { jar -> add(jar.name) }
            }
            val classPath = list.joinToString(",")

            setManifestAttribute("Bundle-ClassPath", classPath)
        }
    }

    private fun setManifestAttribute(name: String, value: String?) {
        if (!jar.manifest.attributes.containsKey(name)) {
            if (!value.isNullOrBlank()) {
                jar.manifest.attributes(mapOf(name to value))
            }
        }
    }

    private fun resolveEmbedJars() {
        embedJarsDir.deleteRecursively()
        GFileUtils.mkdirs(embedJarsDir)

        embedJars.forEach { FileUtils.copyFileToDirectory(it, embedJarsDir) }
    }

    @TaskAction
    fun bundle() {
        resolveEmbedJars()
    }

}