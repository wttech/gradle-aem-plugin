package com.cognifide.gradle.aem.bundle

import com.cognifide.gradle.aem.api.AemDefaultTask
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar
import java.io.File

open class BundleTask : AemDefaultTask() {

    companion object {
        const val NAME = "aemBundle"
    }

    init {
        description = "Prepare resources before composing a JAR and running BND tool."
    }

    @get:Internal
    val jar: Jar
        get() = project.tasks.getByName(JavaPlugin.JAR_TASK_NAME) as Jar

    val embeddableJars: List<File>
        @InputFiles
        get() {
            return project.configurations.getByName(BundlePlugin.CONFIG_EMBED).files.sortedBy { it.name }
        }

    init {
        project.afterEvaluate { configureEmbedJars() }
    }

    private fun configureEmbedJars() {
        if (embeddableJars.isEmpty()) {
            return
        }

        project.logger.info("Embedding jar files: ${embeddableJars.map { it.name }}")

        jar.from(embeddableJars)
        jar.doFirst {
            val list = mutableListOf(".").apply { embeddableJars.forEach { jar -> add(jar.name) } }
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

    @TaskAction
    fun bundle() {
        // nothing to do in execution phase right now, hook for later ;)
    }

}