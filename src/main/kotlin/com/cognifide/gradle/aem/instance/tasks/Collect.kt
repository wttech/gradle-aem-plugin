package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.base.BaseExtension
import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.pkg.tasks.Compose
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.bundling.ZipEntryCompression
import java.io.File

open class Collect : Zip(), AemTask {

    @Nested
    final override val aem = BaseExtension.of(project)

    init {
        group = AemTask.GROUP
        description = "Composes CRX package from all CRX packages being satisfied and built."

        baseName = aem.baseName
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
    private val satisfy = (project.tasks.getByName(Satisfy.NAME) as Satisfy)

    @get:Internal
    val satisfiedPackages: List<File>
        get() = satisfy.outputDirs

    @get:Internal
    val builtPackages: List<File>
        get() = listOf() // TODO AemConfig.pkgs(project).map { it.archivePath }

    override fun projectsEvaluated() {
        project.allprojects.forEach { subproject ->
            if (subproject.plugins.hasPlugin(PackagePlugin.ID)) {
                dependsOn(Compose.NAME)
            }
        }
    }

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