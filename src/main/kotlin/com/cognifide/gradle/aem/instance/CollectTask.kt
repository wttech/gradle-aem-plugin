package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.base.api.AemConfig
import com.cognifide.gradle.aem.base.api.AemTask
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.bundling.ZipEntryCompression
import java.io.File

open class CollectTask : Zip() {

    companion object {
        val NAME = "aemCollect"
    }

    init {
        group = AemTask.GROUP
        description = "Composes CRX package from all CRX packages being satisfied and built."

        classifier = "packages"
        isZip64 = true
        duplicatesStrategy = DuplicatesStrategy.FAIL
        entryCompression = ZipEntryCompression.STORED

        project.gradle.projectsEvaluated({
            from(satisfiedPackages, packageFilter)
            from(builtPackages, packageFilter)
        })
    }

    @Internal
    var packageFilter: ((CopySpec) -> Unit) = { spec ->
        spec.exclude("**/*.lock")
    }

    @get:Internal
    private val satisfy = (project.tasks.getByName(SatisfyTask.NAME) as SatisfyTask)

    @get:Internal
    val satisfiedPackages: List<File>
        get() = satisfy.packageProvider.outputDirs(satisfy.groupFilter)

    @get:Internal
    val builtPackages: List<File>
        get() = AemConfig.pkgs(project).map { it.archivePath }

    override fun copy() {
        resolvePackages()
        super.copy()
    }

    private fun resolvePackages() {
        satisfy.packageProvider.allFiles(satisfy.groupFilter)
    }

}