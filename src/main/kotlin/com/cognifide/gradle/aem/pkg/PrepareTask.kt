package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.internal.file.FileOperations
import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

open class PrepareTask : AemDefaultTask() {

    companion object {
        val NAME = "aemPrepare"
    }

    @OutputDirectory
    val vaultDir = AemTask.temporaryDir(project, NAME, PackagePlugin.VLT_PATH)

    @OutputDirectory
    val jcrRoot = AemTask.temporaryDir(project, NAME, PackagePlugin.JCR_ROOT)

    init {
        description = "Prepare Vault files before composing CRX package"

        project.afterEvaluate {
            config.vaultFilesDirs.forEach { dir -> inputs.dir(dir) }
        }
    }

    @TaskAction
    fun prepare() {
        copyContentVaultFiles()
        copyMissingVaultFiles()
        createEmptyJcrRoot();
    }

    private fun createEmptyJcrRoot() {
        if(jcrRoot.exists()){
            jcrRoot.deleteRecursively()
        }
        jcrRoot.mkdir()
    }


    private fun copyContentVaultFiles() {
        if (vaultDir.exists()) {
            vaultDir.deleteRecursively()
        }
        vaultDir.mkdirs()

        val dirs = config.vaultFilesDirs

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

        FileOperations.copyResources(PackagePlugin.VLT_PATH, vaultDir, true)
    }
}