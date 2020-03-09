package com.cognifide.gradle.aem.pkg.tasks.compose

import com.cognifide.gradle.aem.common.pkg.PackageFile
import com.cognifide.gradle.aem.pkg.tasks.PackageCompose
import com.cognifide.gradle.common.build.DependencyFile
import org.gradle.api.tasks.Input

class PackageNestedResolved(private val target: PackageCompose, @Input val notation: Any) : PackageNested {

    private val aem = target.aem

    private val resolvedFile by lazy { aem.common.resolveFile(DependencyFile.hintNotation(notation, "zip")) }

    override val file = aem.obj.file { fileProvider(aem.obj.provider { resolvedFile }) }

    override val dirPath = aem.obj.string { convention(target.nestedPath.map { "$it/${PackageFile(resolvedFile).group}" }) }

    override val fileName = aem.obj.string { convention(aem.obj.provider { resolvedFile.name }) }

    override val vaultFilter = aem.obj.boolean { convention(target.vaultFilters) }
}
