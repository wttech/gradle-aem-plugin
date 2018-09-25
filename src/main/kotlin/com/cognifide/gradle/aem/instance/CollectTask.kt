package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.api.AemTask
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.bundling.ZipEntryCompression
import java.io.File

open class CollectTask : Zip(), AemTask {

    @Nested
    final override val config = AemConfig.of(project)

    init {
        group = AemTask.GROUP
        description = "Composes CRX package from all CRX packages being satisfied and built."

        baseName = AemConfig.pkgFileName(project)
        classifier = "packages"
        isZip64 = true
        duplicatesStrategy = DuplicatesStrategy.FAIL
        entryCompression = ZipEntryCompression.STORED

        project.gradle.projectsEvaluated {
            from(satisfiedPackages, packageFilter)
            from(builtPackages, packageFilter)
        }
    }

    @Internal
    var packageFilter: ((CopySpec) -> Unit) = { spec ->
        spec.exclude("**/*.lock")
    }

    @Internal
    private val satisfy = (project.tasks.getByName(SatisfyTask.NAME) as SatisfyTask)

    @get:Internal
    val satisfiedPackages: List<File>
        get() = satisfy.outputDirs

    @get:Internal
    val builtPackages: List<File>
        get() = AemConfig.pkgs(project).map { it.archivePath }

    override fun copy() {
        resolvePackages()
        super.copy()
    }

    private fun resolvePackages() {
        satisfy.allFiles
    }

    companion object {
        const val NAME = "aemCollect"
    }

}