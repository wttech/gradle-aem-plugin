package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.AemTask
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.pkg.tasks.Compose
import java.io.File
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.bundling.ZipEntryCompression

open class Collect : Zip(), AemTask {

    @Nested
    final override val aem = AemExtension.of(project)

    @Internal
    var packageFilter: ((CopySpec) -> Unit) = { spec ->
        spec.include("**/.zip")
    }

    @Internal
    var packageCollector: () -> Unit = {
        from(satisfiedPackages, packageFilter)
        from(builtPackages, packageFilter)
    }

    private val satisfy = (project.tasks.getByName(Satisfy.NAME) as Satisfy)

    @get:Internal
    val satisfiedPackages: List<File>
        get() = satisfy.outputDirs

    @get:Internal
    val builtPackages: List<File>
        get() = aem.packagesDependent(this)

    init {
        group = AemTask.GROUP
        description = "Composes CRX package from all CRX packages being satisfied and built."

        baseName = aem.baseName
        classifier = "collection"
        isZip64 = true
        duplicatesStrategy = DuplicatesStrategy.FAIL
        entryCompression = ZipEntryCompression.STORED

        project.gradle.projectsEvaluated { packageCollector() }
    }

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