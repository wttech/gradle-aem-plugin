package com.cognifide.gradle.aem

import org.gradle.api.Project
import org.gradle.util.GFileUtils
import java.io.File

/**
 * @since 1.3  Task configuration cannot be directly modified, because was confusing for plugin users.
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
    }

    val config: AemConfig

}