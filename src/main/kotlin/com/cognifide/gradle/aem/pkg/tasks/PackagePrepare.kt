package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.common.pkg.vlt.FilterFile
import com.cognifide.gradle.aem.common.pkg.vlt.NodeTypesSync
import org.apache.commons.io.FileUtils
import org.apache.jackrabbit.vault.packaging.PackageException
import org.gradle.api.tasks.*
import java.io.File

open class PackagePrepare : AemDefaultTask() {

    /**
     * Ensures that for directory 'META-INF/vault' default files will be generated when missing:
     * 'config.xml', 'filter.xml', 'privileges.xml', 'properties.xml' and 'settings.xml'.
     */
    @Input
    var metaDefaults: Boolean = true

    @OutputDirectory
    val metaDir = aem.temporaryFile("$name/${Package.META_PATH}")

    @get:Internal
    val vaultFilterOriginFile get() = File(metaDir, "${Package.VLT_DIR}/${FilterFile.ORIGIN_NAME}")

    @get:Internal
    val vaultFilterTemplateFile get() = File(metaDir, "${Package.VLT_DIR}/${FilterFile.BUILD_NAME}")

    @Internal
    var contentDir: File = aem.packageOptions.contentDir

    @get:InputFiles
    val metaDirs: List<File> get() = listOf(
            aem.packageOptions.metaCommonDir,
            File(contentDir, Package.META_PATH)
    ).filter { it.exists() }

    @Input
    var vaultNodeTypesSync: NodeTypesSync = aem.packageOptions.nodeTypesSync

    @OutputFile
    var vaultNodeTypesSyncFile = aem.packageOptions.nodeTypesSyncFile

    /**
     * @see <https://github.com/Adobe-Consulting-Services/acs-aem-commons/blob/master/ui.apps/src/main/content/META-INF/vault/nodetypes.cnd>
     */
    private val nodeTypeFallback: String
        get() = FileOperations.readResource(Package.NODE_TYPES_SYNC_PATH)
                ?.bufferedReader()?.readText()
                ?: throw AemException("Cannot read fallback resource for node types!")

    @TaskAction
    fun prepare() {
        syncNodeTypes()
        copyMetaFiles()
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

        if (vaultFilterTemplateFile.exists() && !vaultFilterOriginFile.exists()) {
            vaultFilterTemplateFile.renameTo(vaultFilterOriginFile)
        }

        if (metaDefaults) {
            logger.info("Providing package metadata files in directory: '$metaDir")
            FileOperations.copyResources(Package.META_RESOURCES_PATH, metaDir, true)
        }
    }

    private fun syncNodeTypes() {
        when (vaultNodeTypesSync) {
            NodeTypesSync.ALWAYS -> syncNodeTypesOrElse {
                throw PackageException("Cannot synchronize node types because none of AEM instances are available!")
            }
            NodeTypesSync.AUTO -> syncNodeTypesOrFallback()
            NodeTypesSync.PRESERVE_AUTO -> {
                if (!vaultNodeTypesSyncFile.exists()) {
                    syncNodeTypesOrFallback()
                }
            }
            NodeTypesSync.FALLBACK -> syncNodeTypesFallback()
            NodeTypesSync.PRESERVE_FALLBACK -> {
                if (!vaultNodeTypesSyncFile.exists()) {
                    syncNodeTypesFallback()
                }
            }
            NodeTypesSync.NEVER -> {}
        }
    }

    fun syncNodeTypesOrElse(action: () -> Unit) = aem.buildScope.doOnce("syncNodeTypes") {
        aem.availableInstance?.sync {
            try {
                vaultNodeTypesSyncFile.apply {
                    parentFile.mkdirs()
                    writeText(crx.nodeTypes)
                }
            } catch (e: AemException) {
                aem.logger.debug("Cannot synchronize node types using $instance! Cause: ${e.message}", e)
                action()
            }
        } ?: action()
    }

    fun syncNodeTypesOrFallback() = syncNodeTypesOrElse {
        aem.logger.debug("Using fallback instead of synchronizing node types (forced or AEM instances are unavailable).")
        syncNodeTypesFallback()
    }

    fun syncNodeTypesFallback() = vaultNodeTypesSyncFile.writeText(nodeTypeFallback)

    init {
        description = "Prepares CRX package before composing."
    }

    companion object {
        const val NAME = "packagePrepare"
    }
}
