package com.cognifide.gradle.aem.pkg.tasks.compose

import com.cognifide.gradle.aem.bundle.tasks.bundle
import com.cognifide.gradle.aem.common.pkg.vault.FilterType
import com.cognifide.gradle.aem.pkg.tasks.PackageCompose
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar

class BundleInstalledBuilt(target: PackageCompose, private val task: TaskProvider<Jar>) : BundleInstalled {

    private val aem = target.aem

    override val file = aem.obj.file { convention(task.flatMap { it.archiveFile }) }

    override val dirPath = aem.obj.string { convention(task.flatMap { it.bundle.installPath }) }

    override val fileName = aem.obj.string { convention(task.flatMap { it.archiveFileName }) }

    override val vaultFilter = aem.obj.boolean { convention(task.flatMap { it.bundle.vaultFilter }) }

    override val vaultFilterType = aem.obj.typed<FilterType> { convention(FilterType.FILE) }

    override val runMode = aem.obj.string { convention(task.flatMap { it.bundle.installRunMode }) }

    override val startLevel = aem.obj.int { convention(task.flatMap { it.bundle.installStartLevel }) }
}
