package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.common.asset.AssetManager
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.common.pkg.vault.FilterFile
import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.*

open class PackagePrepare : AemDefaultTask() {

    /**
     * Ensures that for directory 'META-INF/vault' default files will be generated when missing:
     * 'config.xml', 'filter.xml', 'privileges.xml', 'properties.xml' and 'settings.xml'.
     */
    @Input
    val metaDefaults = aem.obj.boolean { convention(true) }

    @Internal
    val metaDir = aem.obj.buildDir("$name/${Package.META_PATH}")

    @Internal
    val vaultFilterOriginFile = aem.obj.relativeFile(metaDir, "${Package.VLT_DIR}/${FilterFile.ORIGIN_NAME}")

    @Internal
    val vaultFilterTemplateFile = aem.obj.relativeFile(metaDir, "${Package.VLT_DIR}/${FilterFile.BUILD_NAME}")

    @Internal
    val contentDir = aem.obj.dir { convention(aem.packageOptions.contentDir) }

    @InputFiles
    val metaDirSources = aem.obj.files {
        from(aem.packageOptions.metaCommonDir)
        from(contentDir.dir(Package.META_PATH))
    }

    @OutputDirectory
    val metaDirTarget = aem.obj.dir { set(metaDir) }

    @TaskAction
    fun prepare() {
        copyMetaFiles()
    }

    private fun copyMetaFiles() {
        val targetDir = metaDir.get().asFile.apply {
            if (exists()) {
                deleteRecursively()
                mkdirs()
            }
        }

        val sourceDirs = metaDirSources.filter { it.exists() }
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
            aem.assetManager.copyDir(AssetManager.META_PATH, targetDir, false)
        }
    }

    init {
        description = "Prepares CRX package before composing."
    }

    companion object {
        const val NAME = "packagePrepare"
    }
}
