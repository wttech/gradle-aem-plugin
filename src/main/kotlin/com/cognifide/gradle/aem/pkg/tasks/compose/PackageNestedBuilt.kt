package com.cognifide.gradle.aem.pkg.tasks.compose

import com.cognifide.gradle.aem.pkg.tasks.PackageCompose
import org.gradle.api.tasks.TaskProvider

class PackageNestedBuilt(target: PackageCompose, private val task: TaskProvider<PackageCompose>) : PackageNested {

    private val aem = target.aem

    override val file = aem.obj.file { convention(task.flatMap { it.archiveFile }) }

    override val dirPath = aem.obj.string { convention(task.flatMap { t -> t.nestedPath.map { "$it/${t.vaultDefinition.group.get()}" } }) }

    override val fileName = aem.obj.string { convention(task.flatMap { it.archiveFileName }) }

    override val vaultFilter = aem.obj.boolean { convention(task.flatMap { it.vaultFilters }) }
}
