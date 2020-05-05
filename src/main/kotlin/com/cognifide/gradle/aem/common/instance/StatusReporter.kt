package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemVersion
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.common.utils.Formats
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class StatusReporter(private val aem: AemExtension) {

    private val logger = aem.logger

    val packages = aem.obj.files {
        from(aem.obj.provider { aem.packagesBuilt.map { it.archiveFile.get().asFile } })
    }

    private val packageFiles by lazy { packages.files.toList() }

    fun init() {
        logger.info("Packages considered in instance status checking (${packageFiles.size}):\n" +
                packageFiles.joinToString("\n"))
    }

    fun report(instance: Instance): String {
        val writer = StringWriter()
        PrintWriter(writer).apply {
            println("Instance '${instance.name}'")
            println(instance.details().prependIndent("  "))

            if (instance.available) {
                println("  Packages installed:")
                println(instance.packagesInstalled(packageFiles).prependIndent("    "))
            }
        }

        return writer.toString()
    }

    private fun Instance.details() = mutableListOf<String>().apply {
        add("URL: $httpUrl (${if (available) "available" else "unavailable"})")
        if (version != AemVersion.UNKNOWN) {
            add("Version: $version")
        }

        if (this@details is LocalInstance) {
            add("Status: ${status.displayName}")
            add("Debug port: $debugPort")
            if (pid > 0) add("Process ID: $pid")
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
    private fun Instance.packagesInstalled(packageFiles: List<File>) = if (available) {
        try {
            sync {
                packageManager.listRetry.never()
                packageFiles
                        .map { file -> PackageFile(file, if (file.exists()) packageManager.find(file) else null) }
                        .sortedWith(
                                compareByDescending<PackageFile> {
                                    it.pkg?.lastUnpacked ?: 0L
                                }.thenBy { it.file.name }
                        )
                        .joinToString("\n") { (file, pkg) ->
                            when {
                                !file.exists() -> "${file.name} (not built)"
                                pkg == null || !pkg.installed -> "${file.name} (not yet)"
                                else -> "${file.name} (${Formats.date(date(pkg.installedTimestamp))})"
                            }
                        }
            }.ifBlank { "none" }
        } catch (e: Exception) {
            logger.debug("Installed packages error", e)
            "error - ${e.message}"
        }
    } else {
        "unknown"
    }

    data class PackageFile(val file: File, val pkg: Package?)
}
