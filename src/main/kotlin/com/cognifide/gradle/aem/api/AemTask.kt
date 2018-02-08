package com.cognifide.gradle.aem.api

import org.gradle.api.Project
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

        fun temporaryDir(project: Project, taskName: String, path: String): File {
            val dir = File(project.buildDir, "aem/$taskName/$path")

            GFileUtils.mkdirs(dir)

            return dir
        }

        fun temporaryFile(project: Project, taskName: String, name: String): File {
            val dir = File(project.buildDir, "aem/$taskName")

            GFileUtils.mkdirs(dir)

            return File(dir, name)
        }
    }

    val config: AemConfig

}