package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.AemTask
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GFileUtils
import org.reflections.Reflections
import org.reflections.scanners.ResourcesScanner
import java.io.File
import java.io.FileOutputStream

open class PrepareTask : DefaultTask(), AemTask {

    companion object {
        val NAME = "aemPrepare"
    }

    @Nested
    final override val config = AemConfig.of(project)

    @OutputDirectory
    val vaultDir = AemTask.temporaryDir(project, NAME, AemPlugin.VLT_PATH)

    init {
        description = "Prepare Vault files before composing CRX package"
        group = AemTask.GROUP

        GFileUtils.mkdirs(vaultDir)
    }

    @TaskAction
    fun prepare() {
        copyContentVaultFiles()
        copyMissingVaultFiles()
    }

    private fun copyContentVaultFiles() {
        if (vaultDir.exists()) {
            vaultDir.deleteRecursively()
        }
        vaultDir.mkdirs()

        val dirs = config.vaultFilesDirs.filter { it.exists() }

        if (dirs.isEmpty()) {
            logger.info("None of Vault files directories exist: $dirs. Only generated defaults will be used.")
        } else {
            dirs.onEach { dir ->
                logger.info("Copying Vault files from path: '${dir.absolutePath}'")

                FileUtils.copyDirectory(dir, vaultDir)
            }
        }
    }

    private fun copyMissingVaultFiles() {
        if (!config.vaultCopyMissingFiles) {
            return
        }

        for (resourcePath in Reflections("${AemPlugin.PKG}.${AemPlugin.VLT_PATH}".replace("/", "."), ResourcesScanner()).getResources { true; }) {
            val outputFile = File(vaultDir, resourcePath.substringAfterLast("${AemPlugin.VLT_PATH}/"))
            if (!outputFile.exists()) {
                val input = javaClass.getResourceAsStream("/" + resourcePath)
                val output = FileOutputStream(outputFile)

                try {
                    IOUtils.copy(input, output)
                } finally {
                    IOUtils.closeQuietly(input)
                    IOUtils.closeQuietly(output)
                }
            }
        }
    }
}