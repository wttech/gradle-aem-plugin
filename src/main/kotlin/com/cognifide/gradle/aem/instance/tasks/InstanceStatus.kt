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
        instanceManager.examine(instances.get())

        val table = common.progress(instances.get().size) {
            AsciiTable().apply {
                context.width = 160

                addRule()
                addRow("Instance", "Packages installed")
                addRule()

                instances.get().onEachApply {
                    increment("Checking status of instance '$name'") {
                        val instanceDetails = mutableListOf<String>().apply {
                            add("Name: $name")
                            add("HTTP URL: $httpUrl (${if (available) "available" else "not available"})")

                            if (this@onEachApply is LocalInstance) {
                                add("Created at: ${if (created) Formats.relativePath(dir.path, project.rootProject.projectDir.path) else "not yet"}")
                            }

                            if (available) {
                                add("State check: $state")
                            }
                        }.joinToString("<br>")

                        val packagesInstalled = if (available) {
                            sync {
                                aem.packagesBuilt.mapNotNull { packageManager.find(it.vaultDefinition) }
                            }.filter { it.installed }.joinToString("<br>") { it.dependencyNotation }.ifBlank { "none" }
                        } else {
                            "unknown"
                        }

                        addRow(instanceDetails, packagesInstalled)
                        addRule()
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
