package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.LocalInstance
import com.cognifide.gradle.aem.common.tasks.InstanceTask
import com.cognifide.gradle.aem.pkg.tasks.PackageCompose
import com.cognifide.gradle.common.utils.Formats
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.PrintWriter
import java.io.StringWriter

open class InstanceStatus : InstanceTask() {

    @Internal
    val packagesBuilt = aem.obj.list<PackageCompose> {
        convention(aem.obj.provider { aem.packagesBuilt })
    }

    @Internal
    val packages = aem.obj.files()

    @Suppress("MagicNumber")
    @TaskAction
    fun status() {
        if (instances.get().isEmpty()) {
            println("No instances defined!")
            return
        }

        common.progress(instances.get().size) {
            common.parallel.each(instances.get()) { instance ->
                increment("Checking status of instance '${instance.name}'") {
                    val writer = StringWriter()
                    PrintWriter(writer).apply {
                        println("Instance '${instance.name}'")

                        println(instance.instanceDetails().prependIndent("  "))
                        println("  Packages installed:")
                        println(instance.packagesInstalled().prependIndent("    "))
                    }
                    println(writer.toString())
                }
            }
        }
    }

    private fun Instance.instanceDetails() = mutableListOf<String>().apply {
        add("URL: $httpUrl (${if (available) "available" else "unavailable"})")
        add("Version: $version")

        if (this@instanceDetails is LocalInstance) {
            add("Status: ${status.displayName}")
            add("Debug port: $debugPort")
        }

        if (available) {
            add("State check: $state")
            add("Run path: $runningPath")
            add("Run modes: ${runningModes.joinToString(",")}")

            add("Time zone: ${zoneId.id} (GMT${zoneOffset.id})")
            add("Operating system: $osInfo")
            add("Java: $javaInfo")
        }
    }.joinToString("\n")

    @Suppress("TooGenericExceptionCaught")
    private fun Instance.packagesInstalled() = if (available) {
        try {
            sync {
                packageManager.listRetry.never()

                val result = mutableListOf<String>()
                result.addAll(packagesBuilt.get().sortedBy { it.path }.mapNotNull { task ->
                    val pkg = packageManager.find(task.vaultDefinition)
                    val path = task.path.removeSuffix(":${task.name}")

                    if (pkg != null && pkg.installed) {
                        "$path (${Formats.date(date(pkg.lastUnpacked!!))})"
                    } else {
                        "$path (not yet)"
                    }
                })
                result.addAll(packages.files.sortedBy { it.name }.mapNotNull { file ->
                    val pkg = packageManager.find(file)
                    val name = file.name

                    if (pkg != null && pkg.installed) {
                        "$name (${Formats.date(date(pkg.lastUnpacked!!))})"
                    } else {
                        "$name (not yet)"
                    }
                })

                result.joinToString("\n")
            }.ifBlank { "none" }
        } catch (e: Exception) {
            logger.debug("Installed packages error", e)
            "error - ${e.message}"
        }
    } else {
        "unknown"
    }

    init {
        description = "Prints status of AEM instances and installed packages."
    }

    companion object {
        const val NAME = "instanceStatus"
    }
}
