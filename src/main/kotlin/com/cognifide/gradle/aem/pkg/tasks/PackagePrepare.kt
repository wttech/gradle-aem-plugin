package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.common.pkg.vlt.FilterFile
import com.cognifide.gradle.aem.common.pkg.vlt.NodeTypesSync
import com.cognifide.gradle.common.CommonException
import org.apache.commons.io.FileUtils
import org.apache.jackrabbit.vault.packaging.PackageException
import org.gradle.api.tasks.*

open class PackagePrepare : AemDefaultTask() {

    /**
     * Ensures that for directory 'META-INF/vault' default files will be generated when missing:
     * 'config.xml', 'filter.xml', 'privileges.xml', 'properties.xml' and 'settings.xml'.
     */
    @Input
    val metaDefaults = aem.obj.boolean { convention(true) }

    @OutputDirectory
    val metaDir = aem.obj.buildDir("$name/${Package.META_PATH}")

    @Internal
    val vaultFilterOriginFile = aem.obj.relativeFile(metaDir, "${Package.VLT_DIR}/${FilterFile.ORIGIN_NAME}")

    @Internal
    val vaultFilterTemplateFile = aem.obj.relativeFile(metaDir, "${Package.VLT_DIR}/${FilterFile.BUILD_NAME}")

    @Internal
    val contentDir = aem.obj.dir { convention(aem.packageOptions.contentDir) }

    @InputFiles
    val metaDirs = aem.obj.files {
        from(aem.packageOptions.metaCommonDir)
        from(contentDir.dir(Package.META_PATH))
    }

    @Input
    val vaultNodeTypesSync = aem.obj.typed<NodeTypesSync> { convention(aem.packageOptions.nodeTypesSync) }

    @OutputFile
    val vaultNodeTypesSyncFile = aem.obj.file { convention(aem.packageOptions.nodeTypesSyncFile) }

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
        val targetDir = metaDir.get().asFile.apply {
            if (exists()) {
                deleteRecursively()
                mkdirs()
            }
        }

        val sourceDirs = metaDirs.filter { it.exists() }
        if (sourceDirs.isEmpty) {
            logger.info("None of package metadata directories exist: $sourceDirs. Only generated defaults will be used.")
        } else {
            sourceDirs.onEach { dir ->
                logger.info("Copying package metadata files from path: '$dir'")

                FileUtils.copyDirectory(dir, targetDir)
            }
        }

        if (vaultFilterTemplateFile.get().asFile.exists() && !vaultFilterOriginFile.get().asFile.exists()) {
            vaultFilterTemplateFile.get().asFile.renameTo(vaultFilterOriginFile.get().asFile)
        }

        if (metaDefaults.get()) {
            logger.info("Providing package metadata files in directory: '$targetDir")
            FileOperations.copyResources(Package.META_RESOURCES_PATH, targetDir, true)
        }
    }

    private fun syncNodeTypes() {
        when (vaultNodeTypesSync.get()) {
            NodeTypesSync.ALWAYS -> syncNodeTypesOrElse {
                throw PackageException("Cannot synchronize node types because none of AEM instances are available!")
            }
            NodeTypesSync.AUTO -> syncNodeTypesOrFallback()
            NodeTypesSync.PRESERVE_AUTO -> {
                if (!vaultNodeTypesSyncFile.get().asFile.exists()) {
                    syncNodeTypesOrFallback()
                }
            }
            NodeTypesSync.FALLBACK -> syncNodeTypesFallback()
            NodeTypesSync.PRESERVE_FALLBACK -> {
                if (!vaultNodeTypesSyncFile.get().asFile.exists()) {
                    syncNodeTypesFallback()
                }
            }
            NodeTypesSync.NEVER -> {}
            null -> {}
        }
    }

    fun syncNodeTypesOrElse(action: () -> Unit) = common.buildScope.doOnce("syncNodeTypes") {
        aem.availableInstance?.sync {
            try {
                vaultNodeTypesSyncFile.get().asFile.apply {
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

    fun syncNodeTypesFallback() = vaultNodeTypesSyncFile.get().asFile.writeText(nodeTypeFallback)

    init {
        description = "Prepares CRX package before composing."
    }

    companion object {
        const val NAME = "packagePrepare"
    }
}
