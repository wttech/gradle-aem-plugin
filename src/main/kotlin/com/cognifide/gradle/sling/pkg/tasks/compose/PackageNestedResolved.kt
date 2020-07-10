package com.cognifide.gradle.sling.pkg.tasks.compose

import com.cognifide.gradle.sling.common.pkg.PackageFile
import com.cognifide.gradle.sling.common.pkg.vault.FilterType
import com.cognifide.gradle.sling.pkg.tasks.PackageCompose
import com.cognifide.gradle.common.build.DependencyFile
import org.gradle.api.tasks.Input

class PackageNestedResolved(private val target: PackageCompose, @Input val notation: Any) : PackageNested {

    private val sling = target.sling

    private val resolvedFile by lazy { sling.common.resolveFile(DependencyFile.hintNotation(notation, "zip")) }

    override val file = sling.obj.file { fileProvider(sling.obj.provider { resolvedFile }) }

    override val dirPath = sling.obj.string { convention(target.nestedPath.map { "$it/${PackageFile(resolvedFile).group}" }) }

    override val fileName = sling.obj.string { convention(sling.obj.provider { resolvedFile.name }) }

    override val vaultFilter = sling.obj.boolean { convention(target.vaultFilters) }

    override val vaultFilterType = sling.obj.typed<FilterType> { convention(FilterType.FILE) }
}
