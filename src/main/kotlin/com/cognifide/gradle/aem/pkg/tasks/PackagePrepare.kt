package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.common.pkg.vlt.FilterFile
import com.cognifide.gradle.aem.common.pkg.vlt.NodeTypesSync
import com.cognifide.gradle.common.CommonException
import com.cognifide.gradle.common.build.dir
import com.cognifide.gradle.common.build.file
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
    val metaDir = aem.obj.buildDir("$name/${Package.META_PATH}")

    @get:Internal
    val vaultFilterOriginFile = aem.obj.relativeFile(metaDir, "${Package.VLT_DIR}/${FilterFile.ORIGIN_NAME}")

    @get:Internal
    val vaultFilterTemplateFile = aem.obj.relativeFile(metaDir, "${Package.VLT_DIR}/${FilterFile.BUILD_NAME}")

    @Internal // TODO @InputDir
    val contentDir = aem.obj.dir(aem.packageOptions.contentDir)

    @get:InputFiles // TODO do not evaluate lazy by it own
    val metaDirs: List<File> get() = listOf(
            aem.packageOptions.metaCommonDir.dir,
            contentDir.dir(Package.META_PATH).dir
    ).filter { it.exists() }

    @Input
    var vaultNodeTypesSync: NodeTypesSync = aem.packageOptions.nodeTypesSync

    @OutputFile
    val vaultNodeTypesSyncFile = aem.obj.file(aem.packageOptions.nodeTypesSyncFile)

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
        val targetDir = metaDir.dir
        if (targetDir.exists()) {
            targetDir.deleteRecursively()
        }

        metaDir.get().asFile.mkdirs()

        if (metaDirs.isEmpty()) {
            logger.info("None of package metadata directories exist: $metaDirs. Only generated defaults will be used.")
        } else {
            metaDirs.onEach { dir ->
                logger.info("Copying package metadata files from path: '$dir'")

                FileUtils.copyDirectory(dir, targetDir)
            }
        }

        if (vaultFilterTemplateFile.get().asFile.exists() && !vaultFilterOriginFile.get().asFile.exists()) {
            vaultFilterTemplateFile.get().asFile.renameTo(vaultFilterOriginFile.get().asFile)
        }

        if (metaDefaults) {
            logger.info("Providing package metadata files in directory: '$metaDir")
            FileOperations.copyResources(Package.META_RESOURCES_PATH, targetDir, true)
        }
    }

    private fun syncNodeTypes() {
        when (vaultNodeTypesSync) {
            NodeTypesSync.ALWAYS -> syncNodeTypesOrElse {
                throw PackageException("Cannot synchronize node types because none of AEM instances are available!")
            }
            NodeTypesSync.AUTO -> syncNodeTypesOrFallback()
            NodeTypesSync.PRESERVE_AUTO -> {
                if (!vaultNodeTypesSyncFile.file.exists()) {
                    syncNodeTypesOrFallback()
                }
            }
            NodeTypesSync.FALLBACK -> syncNodeTypesFallback()
            NodeTypesSync.PRESERVE_FALLBACK -> {
                if (!vaultNodeTypesSyncFile.file.exists()) {
                    syncNodeTypesFallback()
                }
            }
            NodeTypesSync.NEVER -> {}
        }
    }

    fun syncNodeTypesOrElse(action: () -> Unit) = common.buildScope.doOnce("syncNodeTypes") {
        aem.availableInstance?.sync {
            try {
                vaultNodeTypesSyncFile.file.apply {
                    parentFile.mkdirs()
                    writeText(crx.nodeTypes)
                }
            } catch (e: CommonException) {
                aem.logger.debug("Cannot synchronize node types using $instance! Cause: ${e.message}", e)
                action()
            }
        } ?: action()
    }

    fun syncNodeTypesOrFallback() = syncNodeTypesOrElse {
        aem.logger.debug("Using fallback instead of synchronizing node types (forced or AEM instances are unavailable).")
        syncNodeTypesFallback()
    }

    fun syncNodeTypesFallback() = vaultNodeTypesSyncFile.file.writeText(nodeTypeFallback)

    init {
        description = "Prepares CRX package before composing."
    }

    companion object {
        const val NAME = "packagePrepare"
    }
}
