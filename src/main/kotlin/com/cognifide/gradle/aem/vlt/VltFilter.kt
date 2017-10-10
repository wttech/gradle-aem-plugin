package com.cognifide.gradle.aem.vlt

import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.internal.file.FileOperations
import org.gradle.api.Project
import java.io.File

class VltFilter(val file: File, private val temporary: Boolean = false) {

    companion object {

        fun temporary(project: Project, paths: List<String>): VltFilter {
            val template = FileOperations.readResource("vlt/path.xml")!!
                    .bufferedReader().use { it.readText() }
            val content = PropertyParser(project).expand(template, mapOf("paths" to paths))
            val file = AemTask.temporaryFile(project, VltTask.NAME, "filter.xml")

            file.printWriter().use { it.print(content) }

            return VltFilter(file, true)
        }

    }

    fun clean() {
        if (temporary) {
            file.delete()
        }
    }

}