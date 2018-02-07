package com.cognifide.gradle.aem.bundle

import aQute.bnd.osgi.Jar
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import java.io.File
import java.util.*

class BundleCollector(val project: Project) {

    val archiveConfig: Configuration?
        get() = project.configurations.findByName(Dependency.ARCHIVES_CONFIGURATION)

    val installConfig: Configuration?
        get() = project.configurations.findByName(BundlePlugin.CONFIG_INSTALL)

    val archiveArtifacts: List<String>
        get() = archiveConfig?.run { allArtifacts.map { it.toString() } } ?: listOf()

    val installDependencies
        get() = installConfig?.run { allDependencies.map { it.toString() } } ?: listOf()

    val all: List<String>
        get() = archiveArtifacts + installDependencies

    val allJars: Collection<File>
        get() {
            val jars = TreeSet<File>()
            archiveConfig?.apply { jars += allArtifacts.files.files }
            installConfig?.apply { jars += resolve().toList() }

            return jars.filter { isOsgiBundle(it) }
        }

    private fun isOsgiBundle(it: File): Boolean {
        return try {
            !Jar(it).manifest.mainAttributes.getValue("Bundle-SymbolicName").isNullOrBlank()
        } catch (e: Exception) {
            false
        }
    }
}
