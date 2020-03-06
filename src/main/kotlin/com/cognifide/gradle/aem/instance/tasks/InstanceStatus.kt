package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.LocalInstance
import com.cognifide.gradle.aem.common.tasks.InstanceTask
import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.common.utils.onEachApply
import de.vandermeer.asciitable.AsciiTable
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment
import org.gradle.api.Incubating
import org.gradle.api.tasks.TaskAction

@Incubating
open class InstanceStatus : InstanceTask() {

    @TaskAction
    @Suppress("MagicNumber")
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
                        val local = this is LocalInstance

                        increment("Checking status of instance '$name'") {
                            val instanceDetails = mutableListOf<String>().apply {
                                add("Name: $name (${if (local) "local" else "remote"})")
                                add("URL: $httpUrl (${if (available) "available" else "not available"})")

                                if (this@onEachApply is LocalInstance) {
                                    add("Debug port: $debugPort")
                                }

                                if (available) {
                                    add("Version: $version")
                                    add("State check: $state")
                                    add("Run path: $runningPath")
                                    add("Run modes: ${runningModes.joinToString(",")}")

                                    if (logger.isInfoEnabled) {
                                        add("Time zone: ${zoneId.id} (GMT${zoneOffset.id})")
                                        add("Operating system: $osInfo")
                                        add("Java: $javaInfo")
                                    }
                                }
                            }.joinToString("<br>")

                            val packagesInstalled = if (available) {
                                sync {
                                    aem.packagesBuilt.map { task ->
                                        sync {
                                            val pkg = packageManager.find(task.vaultDefinition)
                                            if (pkg != null && pkg.installed) {
                                                "${task.path} (${Formats.date(date(pkg.lastUnpacked!!))})"
                                            } else {
                                                "${task.path} (not yet)"
                                            }
                                        }
                                    }.joinToString("<br>")
                                }.ifBlank { "none" }
                            } else {
                                "unknown"
                            }

                            addRow(instanceDetails, packagesInstalled)
                            addRule()
                        }
                    }
                }

                setTextAlignment(TextAlignment.LEFT)
            }
        }

        println(table.render())
    }

    init {
        description = "Prints out status of all AEM instances."
    }

    companion object {
        const val NAME = "instanceStatus"
    }
}
