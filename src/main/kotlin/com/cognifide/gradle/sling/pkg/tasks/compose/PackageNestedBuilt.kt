package com.cognifide.gradle.sling.pkg.tasks.compose

import com.cognifide.gradle.sling.common.pkg.vault.FilterType
import com.cognifide.gradle.sling.pkg.tasks.PackageCompose
import org.gradle.api.tasks.TaskProvider

class PackageNestedBuilt(target: PackageCompose, private val task: TaskProvider<PackageCompose>) : PackageNested {

    private val sling = target.sling

    override val file = sling.obj.file { convention(task.flatMap { it.archiveFile }) }

    override val dirPath = sling.obj.string { convention(task.flatMap { t -> t.nestedPath.map { "$it/${t.vaultDefinition.group.get()}" } }) }

    override val fileName = sling.obj.string { convention(task.flatMap { it.archiveFileName }) }

    override val vaultFilter = sling.obj.boolean { convention(task.flatMap { it.vaultFilters }) }

    override val vaultFilterType = sling.obj.typed<FilterType> { convention(FilterType.FILE) }
}
