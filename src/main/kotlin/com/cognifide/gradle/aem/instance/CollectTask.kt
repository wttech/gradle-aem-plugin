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
        description = "Composes CRX package from all CRX packages being built and satisfied files."

        classifier = "packages"
        isZip64 = true
        duplicatesStrategy = DuplicatesStrategy.FAIL
        entryCompression = ZipEntryCompression.STORED

        project.gradle.projectsEvaluated({
            from(satisfiedFiles, packageFilter)
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
    val satisfiedFiles: List<File>
        get() = satisfy.filesProvider.outputDirs(satisfy.groupFilter)

    @get:Internal
    val builtPackages: List<File>
        get() = AemConfig.pkgs(project).map { it.archivePath }

    override fun copy() {
        resolveFiles()
        super.copy()
    }

    private fun resolveFiles() {
        satisfy.filesProvider.allFiles(satisfy.groupFilter)
    }

}