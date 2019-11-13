package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.common.pkg.vlt.FilterFile
import com.cognifide.gradle.aem.common.pkg.vlt.NodeTypesSync
import org.apache.commons.io.FileUtils
import org.apache.jackrabbit.vault.packaging.PackageException
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
    val vaultFilterOriginFile get() = File(metaDir, "${Package.VLT_DIR}/${FilterFile.ORIGIN_NAME}")

    @get:Internal
    val vaultFilterTemplateFile get() = File(metaDir, "${Package.VLT_DIR}/${FilterFile.BUILD_NAME}")

    @get:InputFiles
    val metaDirs: List<File>
        get() = listOf(
                aem.packageOptions.metaCommonDir,
                File(aem.packageOptions.contentDir, Package.META_PATH)
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
            NodeTypesSync.WHEN_AVAILABLE -> syncNodeTypesOrFallback()
            NodeTypesSync.WHEN_MISSING -> {
                if (!vaultNodeTypesSyncFile.exists()) {
                    syncNodeTypesOrFallback()
                }
            }
            NodeTypesSync.USE_FALLBACK -> syncNodeTypesFallback()
            NodeTypesSync.NEVER -> {}
        }
    }

    fun syncNodeTypesOrElse(action: () -> Unit) {
        aem.buildScope.doOnce("syncNodeTypes") {
            aem.availableInstance?.sync {
                try {
                    vaultNodeTypesSyncFile.apply {
                        GFileUtils.parentMkdirs(this)
                        writeText(crx.nodeTypes)
                    }
                } catch (e: AemException) {
                    aem.logger.debug("Cannot synchronize node types using $instance! Cause: ${e.message}", e)
                }
            } ?: action()
        }
    }

    fun syncNodeTypesOrFallback() = syncNodeTypesOrElse {
        aem.logger.debug("Cannot synchronize node types because none of AEM instances are available! Using fallback instead.")
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
