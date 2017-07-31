package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemPackagePlugin
import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.internal.FileOperations
import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

open class PrepareTask : DefaultTask(), AemTask {

    companion object {
        val NAME = "aemPrepare"
    }

    @Nested
    final override val config = AemConfig.of(project)

    @OutputDirectory
    val vaultDir = AemTask.temporaryDir(project, NAME, AemPackagePlugin.VLT_PATH)

    init {
        description = "Prepare Vault files before composing CRX package"
        group = AemTask.GROUP

        project.afterEvaluate {
            config.vaultFilesDirs.forEach { dir -> inputs.dir(dir) }
        }
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

        FileOperations.copyResources(AemPackagePlugin.VLT_PATH, vaultDir, true)
    }
}