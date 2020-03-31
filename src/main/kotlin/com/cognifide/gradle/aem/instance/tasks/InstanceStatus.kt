package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.LocalInstance
import com.cognifide.gradle.aem.common.tasks.InstanceTask
import com.cognifide.gradle.aem.pkg.tasks.PackageCompose
import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.common.utils.onEachApply
import de.vandermeer.asciitable.AsciiTable
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class InstanceStatus : InstanceTask() {

    @Internal
    val detailed = aem.obj.boolean {
        convention(logger.isInfoEnabled)
        aem.prop.boolean("instance.status.detailed")?.let { set(it) }
    }

    @Internal
    val packagesBuilt = aem.obj.list<PackageCompose> {
        convention(aem.obj.provider { aem.packagesBuilt })
    }

    @Internal
    val packages = aem.obj.files()

    @TaskAction
    @Suppress("MagicNumber", "LongMethod", "TooGenericExceptionCaught")
    fun status() {
        val table = common.progress(instances.get().size) {
            AsciiTable().apply {
                context.width = 160

                addRule()
                addRow("Instance", "Packages installed")
                addRule()

                if (instances.get().isEmpty()) {
                    addRow("none, none")
                    addRule()
                } else {
                    instances.get().onEachApply {
                        increment("Checking status of instance '$name'") {
                            addRow(instanceDetails(), packagesInstalled())
                            addRule()
                        }
                    }
                }

                setTextAlignment(TextAlignment.LEFT)
            }
        }

        println(table.render())
    }

    private fun Instance.instanceDetails() = mutableListOf<String>().apply {
        add("URL: $httpUrl | Available: $available")
        add("Name: $name | Version: $version")

        if (this@instanceDetails is LocalInstance) {
            add("Status: ${status.displayName} | Debug port: $debugPort")
        }

        if (available) {
            add("State check: $state")
            add("Run path: $runningPath")
            add("Run modes: ${runningModes.joinToString(",")}")

            if (detailed.get()) {
                add("Time zone: ${zoneId.id} (GMT${zoneOffset.id})")
                add("Operating system: $osInfo")
                add("Java: $javaInfo")
            }
        }
    }.joinToString("<br>")

    private fun Instance.packagesInstalled() = if (available) {
        try {
            sync {
                packageManager.listRetry.never()
                packagesBuilt.get().sortedBy { it.path }.mapNotNull { task ->
                    val pkg = packageManager.find(task.vaultDefinition)
                    val path = task.path.removeSuffix(":${task.name}")

                    if (pkg != null && pkg.installed) {
                        "$path (${Formats.date(date(pkg.lastUnpacked!!))})"
                    } else {
                        "$path (not yet)"
                    }
                }.joinToString("<br>")
                packages.files.sortedBy { it.name }.mapNotNull { file ->
                    val pkg = packageManager.find(file)
                    val name = file.name

                    if (pkg != null && pkg.installed) {
                        "$name (${Formats.date(date(pkg.lastUnpacked!!))})"
                    } else {
                        "$name (not yet)"
                    }
                }.joinToString("<br>")
            }.ifBlank { "none" }
        } catch (e: Exception) {
            "unknown"
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
