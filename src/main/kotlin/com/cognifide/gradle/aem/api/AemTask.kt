package com.cognifide.gradle.aem.api

import org.gradle.api.Project
import org.gradle.util.GFileUtils
import java.io.File

interface AemTask {

    companion object {
        val GROUP = "AEM"

        fun taskDir(project: Project, taskName: String): File {
            val dir = File(project.buildDir, "aem/$taskName")

            GFileUtils.mkdirs(dir)

            return dir
        }

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