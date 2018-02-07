package com.cognifide.gradle.aem.pkg

import aQute.bnd.osgi.Jar
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import java.io.File
import java.util.*

// TODO archives config returns no jars, still configuration is being resolved
class BundleCollector(val project: Project) {

    val archiveConfig = project.configurations.findByName(Dependency.ARCHIVES_CONFIGURATION)

    val installConfig = project.configurations.findByName(PackagePlugin.CONFIG_INSTALL)

    val archiveDependencies
        get() = archiveConfig?.map { it.toString() } ?: listOf()

    val installDependencies
        get() = installConfig?.map { it.toString() } ?: listOf()

    val allDependencies: List<String>
        get() = archiveDependencies + installDependencies

    val allJars: Collection<File>
        get() {
            val jars = TreeSet<File>()
            jars += archiveConfig?.resolve() ?: listOf()
            jars += installConfig?.resolve() ?: listOf()

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
