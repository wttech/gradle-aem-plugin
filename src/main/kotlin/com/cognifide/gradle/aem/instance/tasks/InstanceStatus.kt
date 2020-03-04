package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.LocalInstance
import com.cognifide.gradle.aem.common.tasks.InstanceTask
import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.common.utils.onEachApply
import de.vandermeer.asciitable.AsciiTable
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment
import org.gradle.api.tasks.TaskAction

open class InstanceStatus : InstanceTask() {

    @TaskAction
    fun status() {
        println(AsciiTable().apply {
            context.width = 160

            addRule()
            addRow("Instance", "Packages installed")
            addRule()

            instances.get().onEachApply {
                var instanceDetails = "Name: $name<br>HTTP URL: $httpUrl<br>Available: $available<br>"
                if (this is LocalInstance) {
                    instanceDetails += "Created: ${if (created) Formats.relativePath(dir.path, project.rootProject.projectDir.path) else "not yet"}"
                }
                val packagesInstalled = if (available) {
                    sync {
                        aem.packagesBuilt.mapNotNull { packageManager.find(it.vaultDefinition) }
                    }.filter { it.installed }.joinToString("<br>") { it.dependencyNotation }.ifBlank { "none" }
                } else {
                    ""
                }

                addRow(instanceDetails, packagesInstalled)
                addRule()
            }

            setTextAlignment(TextAlignment.LEFT)
        }.render())
    }

    init {
        description = "Prints out status of all AEM instances."
    }

    companion object {
        const val NAME = "instanceStatus"
    }
}
