package com.cognifide.gradle.aem.bundle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import java.io.File
import java.util.*

class BundleCollector(val project: Project) {

    val archiveConfig: Configuration?
        get() = project.configurations.findByName(Dependency.ARCHIVES_CONFIGURATION)

    val archiveArtifacts: List<String>
        get() = archiveConfig?.run { allArtifacts.map { it.toString() } } ?: listOf()

    val all: List<String>
        get() = archiveArtifacts

    val allJars: Collection<File>
        get() {
            val jars = TreeSet<File>()

            archiveConfig?.apply { jars += allArtifacts.files.files }

            return jars.filter { it.extension == "jar" }
        }
}
