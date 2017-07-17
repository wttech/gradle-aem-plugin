package com.cognifide.gradle.aem.debug

import com.cognifide.gradle.aem.internal.PropertyParser
import org.gradle.api.Project

class ProjectDumper(@Transient val project: Project) {

    val properties: Map<String, Any>
        get() {
            return mapOf(
                    "project" to mapOf(
                            "path" to project.path,
                            "name" to project.name,
                            "dir" to project.projectDir.absolutePath
                    ),
                    "aem" to PropertyParser(project).aemProperties
            )
        }

}