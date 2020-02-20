package com.cognifide.gradle.aem.pkg.tasks.compose

import com.cognifide.gradle.aem.common.pkg.PackageFile
import com.cognifide.gradle.aem.pkg.tasks.PackageCompose
import com.cognifide.gradle.common.build.DependencyFile
import org.gradle.api.tasks.Input

class PackageNestedResolved(private val target: PackageCompose, @Input val notation: Any) : PackageNested {

    private val aem = target.aem

    private val project = aem.project

    private val dependencyFile = DependencyFile(project, DependencyFile.hintNotation(notation, "zip"))

    override val file = aem.obj.file { fileProvider(aem.obj.provider { dependencyFile.file }) }

    override val dirPath = aem.obj.string { convention(target.nestedPath.map { "$it/${PackageFile(dependencyFile.file).group}" }) }

    override val fileName = aem.obj.string { convention(aem.obj.provider { dependencyFile.file.name }) }

    override val vaultFilter = aem.obj.boolean { convention(target.vaultFilters) }
}
