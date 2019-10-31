package com.cognifide.gradle.aem.common.pkg.validator

import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.common.file.FileOperations
import org.apache.commons.io.FileUtils
import java.io.File

class OpearDir(validator: PackageValidator) {

    private val aem = validator.aem

    private val logger = aem.logger

    var root = AemTask.temporaryDir(aem.project, "package", "opear-dir")

    var plan = File(root, aem.props.string("package.validator.opear.plan") ?: "plan.json")

    private var baseProvider: () -> File? = {
        aem.props.string("package.validator.opear.base")?.let { aem.resolveFile(it) }
    }

    fun base(value: String) = base { aem.resolveFile(value) }

    fun base(provider: () -> File?) {
        this.baseProvider = provider
    }

    private val configDirs = listOf(
            File(aem.configDir, CONFIG_DIR_PATH),
            File(aem.configCommonDir, CONFIG_DIR_PATH)
    )

    fun prepare() {
        logger.info("Preparing OakPAL Opear directory '$root'")

        root.deleteRecursively()
        root.mkdirs()

        val baseFile = baseProvider()
        baseFile?.let { file ->
            logger.info("Extracting OakPAL Opear configuration files from file '$file' to directory '$root'")
            FileOperations.zipUnpackAll(file, root)
        }

        for (configDir in configDirs) {
            if (configDir.exists()) {
                logger.info("Using project-specific OakPAL Opear configuration files from file '$configDir' to directory '$root'")
                FileUtils.copyDirectory(configDir, root)
                break
            }
        }
    }

    companion object {
        const val CONFIG_DIR_PATH = "package/validator/opear-dir"
    }
}
