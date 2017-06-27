package com.cognifide.gradle.aem

import org.gradle.api.Project
import java.io.File

/**
 * @since 1.3  Task configuration cannot be directly modified, because was confusing for plugin users.
 *
 * Instead: aemCompose { config { /* ... */ } }
 * Simply write: aem { config { /* ... */ } }
 */
interface AemTask {

    val config: AemConfig

    companion object {
        fun temporaryDir(project: Project): File {
            return File(project.buildDir, AemPlugin.TASK_TEMP_PATH)
        }

        fun temporaryDir(project: Project, path: String): File {
            return File(project.buildDir, "${AemPlugin.TASK_TEMP_PATH}/$path")
        }
    }

}