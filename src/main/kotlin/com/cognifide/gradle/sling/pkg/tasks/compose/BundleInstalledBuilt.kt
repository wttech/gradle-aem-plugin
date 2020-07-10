package com.cognifide.gradle.sling.pkg.tasks.compose

import com.cognifide.gradle.sling.bundle.tasks.bundle
import com.cognifide.gradle.sling.common.pkg.vault.FilterType
import com.cognifide.gradle.sling.pkg.tasks.PackageCompose
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar

class BundleInstalledBuilt(target: PackageCompose, private val task: TaskProvider<Jar>) : BundleInstalled {

    private val sling = target.sling

    override val file = sling.obj.file { convention(task.flatMap { it.archiveFile }) }

    override val dirPath = sling.obj.string { convention(task.flatMap { it.bundle.installPath }) }

    override val fileName = sling.obj.string { convention(task.flatMap { it.archiveFileName }) }

    override val vaultFilter = sling.obj.boolean { convention(task.flatMap { it.bundle.vaultFilter }) }

    override val vaultFilterType = sling.obj.typed<FilterType> { convention(FilterType.FILE) }

    override val runMode = sling.obj.string { convention(task.flatMap { it.bundle.installRunMode }) }
}
