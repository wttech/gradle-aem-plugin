package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.common.pkg.vlt.FilterFile
import com.cognifide.gradle.aem.common.pkg.vlt.NodeTypesSync
import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.*
import org.gradle.util.GFileUtils
import java.io.File

open class PackagePrepare : AemDefaultTask() {

    /**
     * Ensures that for directory 'META-INF/vault' default files will be generated when missing:
     * 'config.xml', 'filter.xml', 'properties.xml' and 'settings.xml'.
     */
    @Input
    var metaDefaults: Boolean = true

    @OutputDirectory
    val metaDir = AemTask.temporaryDir(project, name, Package.META_PATH)

    @get:Internal
    val filterBackup get() = File(metaDir, "${Package.VLT_DIR}/${FilterFile.ROOTS_NAME}")

    @get:Internal
    val filterTemplate get() = File(metaDir, "${Package.VLT_DIR}/${FilterFile.BUILD_NAME}")

    /**
     * CRX package Vault files will be composed from given sources.
     * Missing files required by package within installation will be auto-generated if 'vaultCopyMissingFiles' is enabled.
     */
    @get:InputFiles
    val metaDirs: List<File>
        get() = listOf(
                aem.packageOptions.metaCommonDir,
                File(aem.packageOptions.contentDir, Package.META_PATH)
        ).filter { it.exists() }

    @Input
    var vaultNodeTypesSync: NodeTypesSync = aem.packageOptions.nodeTypesSync

    @OutputFile
    var vaultNodeTypesFile = aem.packageOptions.nodeTypesFile

    /**
     * @see <https://github.com/Adobe-Consulting-Services/acs-aem-commons/blob/master/ui.apps/src/main/content/META-INF/vault/nodetypes.cnd>
     */
    private val nodeTypeFallback: String
        get() = FileOperations.readResource(Package.NODE_TYPES_EXPORT_PATH)
                ?.bufferedReader()?.readText()
                ?: throw AemException("Cannot read fallback resource for exported node types!")

    @TaskAction
    fun prepare() {
        copyMetaFiles()
        syncNodeTypes()
    }

    private fun copyMetaFiles() {
        if (metaDir.exists()) {
            metaDir.deleteRecursively()
        }

        metaDir.mkdirs()

        if (metaDirs.isEmpty()) {
            logger.info("None of package metadata directories exist: $metaDirs. Only generated defaults will be used.")
        } else {
            metaDirs.onEach { dir ->
                logger.info("Copying package metadata files from path: '$dir'")

                FileUtils.copyDirectory(dir, metaDir)
            }
        }

        if (filterTemplate.exists() && !filterBackup.exists()) {
            filterTemplate.renameTo(filterBackup)
        }

        if (metaDefaults) {
            logger.info("Providing package metadata files in directory: '$metaDir")
            FileOperations.copyResources(Package.META_RESOURCES_PATH, metaDir, true)
        }
    }

    private fun syncNodeTypes() {
        if (vaultNodeTypesSync == NodeTypesSync.ALWAYS ||
                (vaultNodeTypesSync == NodeTypesSync.WHEN_MISSING && !vaultNodeTypesFile.exists())) {
            aem.availableInstance?.sync {
                try {
                    vaultNodeTypesFile.apply {
                        GFileUtils.parentMkdirs(this)
                        writeText(crx.nodeTypes)
                    }
                } catch (e: AemException) {
                    aem.logger.debug("Cannot export and save node types from $instance! Cause: ${e.message}", e)
                }
            } ?: run {
                vaultNodeTypesFile.writeText(nodeTypeFallback)
                aem.logger.debug("No available instances to export node types! Using fallback instead.")
            }
        }
    }

    init {
        description = "Prepares CRX package before composing."
    }

    companion object {
        const val NAME = "packagePrepare"
    }
}
