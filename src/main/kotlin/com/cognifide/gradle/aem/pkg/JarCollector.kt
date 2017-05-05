package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.AemPlugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import java.io.File
import java.util.*

class JarCollector(val project: Project) {

    val projectArtifacts: List<File>
        get() {
            val config = project.configurations.findByName(Dependency.ARCHIVES_CONFIGURATION)
            if (config != null) {
                return config.allArtifacts.files.files.filter { it.name.endsWith(".jar") }
            }

            return listOf()
        }

    fun dependencies(configName: String): List<File> {
        val config = project.configurations.findByName(configName)
        if (config != null) {
            return config.resolve().toList()
        }

        return listOf()
    }

    val all: Collection<File>
        get() {
            val jars = TreeSet<File>()

            jars += projectArtifacts
            jars += dependencies(AemPlugin.CONFIG_INSTALL)

            return jars
        }
}
