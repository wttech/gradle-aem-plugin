package com.cognifide.gradle.aem.pkg.tasks.compose

import com.cognifide.gradle.aem.pkg.tasks.PackageCompose
import com.cognifide.gradle.common.build.DependencyFile
import org.gradle.api.tasks.Input

class BundleInstalledResolved(private val target: PackageCompose, @Input val notation: Any) : BundleInstalled {

    private val aem = target.aem

    private val project = aem.project

    private val dependencyFile = DependencyFile(project, notation)

    override val file = aem.obj.file { fileProvider(aem.obj.provider { dependencyFile.file }) }

    override val dirPath = aem.obj.string { convention(target.bundlePath) }

    override val fileName = aem.obj.string { convention(aem.obj.provider { dependencyFile.file.name }) }

    override val vaultFilter = aem.obj.boolean { convention(target.vaultFilters) }
}
