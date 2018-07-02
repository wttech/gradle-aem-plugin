package com.cognifide.gradle.aem.api

import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip
import org.gradle.util.GFileUtils
import java.io.File

/**
 * @since 1.3 Task configuration should not be directly modified, because it is confusing for plugin users.
 *
 * Instead: aemCompose { config { /* ... */ } }
 * Simply write: aem { config { /* ... */ } }
 */
interface AemTask {

    companion object {
        val GROUP = "AEM"

        fun temporaryDir(project: Project, taskName: String): File {
            val dir = File(project.buildDir, "aem/$taskName")
            GFileUtils.mkdirs(dir)
            return dir
        }

        fun temporaryDir(project: Project, taskName: String, path: String): File {
            val dir = File(temporaryDir(project, taskName), path)
            GFileUtils.mkdirs(dir)
            return dir
        }

        fun temporaryFile(project: Project, taskName: String, name: String): File {
            val dir = File(project.buildDir, "aem/$taskName")

            GFileUtils.mkdirs(dir)

            return File(dir, name)
        }

        fun packageName(task: Zip) {
            return  // TODO packageName + classifier (one liner)
        }
    }

    val config: AemConfig

}

fun Zip.basePackageName(config: AemConfig): String {
    return if (config.projectNameUnique) {
        project.name
    } else {
        "${config.projectNamePrefix}-${project.name}"
    }
}

fun Zip.packageName(config: AemConfig): String {
    return if (config.projectNameUnique) {
        project.name
    } else {
        "${config.projectNamePrefix}-${project.name}"
    }.apply {
        val classifier = classifier
        return if (classifier.isNullOrBlank()) "$this.$classifier" else this
    }
}
