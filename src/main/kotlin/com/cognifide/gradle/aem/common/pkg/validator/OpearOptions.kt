package com.cognifide.gradle.aem.common.pkg.validator

import com.cognifide.gradle.aem.common.file.FileOperations
import org.apache.commons.io.FileUtils
import java.io.File

class OpearOptions(validator: PackageValidator)  {

    private val aem = validator.aem

    private val logger = aem.logger

    var dir = aem.project.file("build/aem/package/validator/opear-dir")

    private var baseProvider: () -> File? = {
        aem.props.string("package.validator.opear.base")?.let { aem.resolveFile(it) }
    }

    fun base(provider: () -> File?) {
        this.baseProvider = provider
    }

    private val configDirs = listOf(
            File(aem.configDir, CONFIG_DIR_PATH),
            File(aem.configCommonDir, CONFIG_DIR_PATH)
    )

    fun prepare() {
        logger.info("Preparing OakPAL Opear directory '$dir'")

        dir.deleteRecursively()
        dir.mkdirs()

        val baseFile = baseProvider()
        baseFile?.let { file ->
            logger.info("Extracting OakPAL Opear configuration files from file '$file' to directory '$dir'")
            FileOperations.zipUnpackAll(file, dir)
        }

        for (configDir in configDirs) {
            if (configDir.exists()) {
                logger.info("Using project-specific OakPAL Opear configuration files from file '$configDir' to directory '$dir'")
                FileUtils.copyDirectory(configDir, dir)
                break
            }
        }
    }

    companion object {
        const val CONFIG_DIR_PATH = "package/validator/opear-dir"
    }
}
