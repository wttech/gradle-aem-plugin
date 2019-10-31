package com.cognifide.gradle.aem.common.pkg.validator

import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.file.resolver.FileResolver
import com.cognifide.gradle.aem.common.utils.Formats
import org.gradle.internal.impldep.org.codehaus.plexus.util.FileUtils
import java.io.File

class OpearOptions(validator: PackageValidator)  {

    private val aem = validator.aem

    var dir = aem.project.file("build/aem/package/validator/opear-dir")

    private var baseResolver: (FileResolver.() -> Unit)? = null

    fun base(resolver: FileResolver.() -> Unit) {
        this.baseResolver = resolver
    }

    val baseFile: File? get() = baseResolver?.let { aem.resolveFile(it) }

    val planFile: File get() = File(dir, "plan.json")

    val configDir = File(aem.configCommonDir, "package/validator/opear-dir")

    private var planOptions: (OakpalPlan.() -> Unit)? = null

    fun plan(options: OakpalPlan.() -> Unit) {
        planOptions = options
    }

    fun prepare() {
        dir.deleteRecursively()
        dir.mkdirs()

        baseFile?.let { FileOperations.zipUnpackAll(it, dir) }

        if (configDir.exists()) {
            FileUtils.copyDirectory(configDir, dir)
        }

        planOptions?.let {
            val plan = OakpalPlan().apply(it)
            val json = Formats.toJson(plan)
            planFile.writeText(json)
        }
    }
}
