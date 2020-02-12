package com.cognifide.gradle.aem.common.pkg.vlt

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import org.apache.commons.lang3.time.StopWatch
import java.io.File

class VltClient(val aem: AemExtension) {

    private val app = VltApp(aem.project)

    val command = aem.obj.string()

    val commandProperties = aem.obj.map<String, Any> { convention(mapOf("aem" to aem)) }

    val commandEffective get() = aem.prop.expand(command.get(), commandProperties.get())

    val contentDir = aem.obj.dir { convention(aem.packageOptions.contentDir) }

    val contentRelativePath = aem.obj.string()

    val contentDirEffective: File
        get() {
            var workingDir = contentDir.map { it.asFile.resolve(Package.JCR_ROOT) }.get()
            if (contentRelativePath.isPresent) {
                workingDir = File(workingDir, contentRelativePath.get())
            }

            return workingDir
        }

    fun run(): VltSummary {
        if (commandEffective.isBlank()) {
            throw VltException("Vault command cannot be blank.")
        }

        aem.logger.lifecycle("Working directory: $contentDirEffective")
        aem.logger.lifecycle("Executing command: vlt $commandEffective")

        val stopWatch = StopWatch().apply { start() }
        app.execute(commandEffective, contentDirEffective)
        stopWatch.stop()

        return VltSummary(commandEffective, contentDirEffective, stopWatch.time)
    }
}
