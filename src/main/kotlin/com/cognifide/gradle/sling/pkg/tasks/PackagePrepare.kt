package com.cognifide.gradle.sling.pkg.tasks

import com.cognifide.gradle.sling.SlingDefaultTask
import com.cognifide.gradle.sling.common.asset.AssetManager
import com.cognifide.gradle.sling.common.instance.service.pkg.Package
import com.cognifide.gradle.sling.common.pkg.vault.FilterFile
import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.*

open class PackagePrepare : SlingDefaultTask() {

    /**
     * Ensures that for directory 'META-INF/vault' default files will be generated when missing:
     * 'config.xml', 'filter.xml', 'privileges.xml', 'properties.xml' and 'settings.xml'.
     */
    @Input
    val metaDefaults = sling.obj.boolean { convention(true) }

    @Internal
    val metaDir = sling.obj.buildDir("$name/${Package.META_PATH}")

    @Internal
    val vaultFilterOriginFile = sling.obj.relativeFile(metaDir, "${Package.VLT_DIR}/${FilterFile.ORIGIN_NAME}")

    @Internal
    val vaultFilterTemplateFile = sling.obj.relativeFile(metaDir, "${Package.VLT_DIR}/${FilterFile.BUILD_NAME}")

    @Internal
    val contentDir = sling.obj.dir { convention(sling.packageOptions.contentDir) }

    @InputFiles
    val metaDirSources = sling.obj.files {
        from(sling.packageOptions.metaCommonDir)
        from(contentDir.dir(Package.META_PATH))
    }

    @OutputDirectory
    val metaDirTarget = sling.obj.dir { set(metaDir) }

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
            sling.assetManager.copyDir(AssetManager.META_PATH, targetDir, false)
        }
    }

    init {
        description = "Prepares CRX package before composing."
    }

    companion object {
        const val NAME = "packagePrepare"
    }
}
