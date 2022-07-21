package com.cognifide.gradle.aem.pkg.tasks.compose

import com.cognifide.gradle.aem.common.pkg.vault.FilterType
import com.cognifide.gradle.aem.pkg.tasks.PackageCompose
import org.gradle.api.tasks.Input

class BundleInstalledResolved(private val target: PackageCompose, @Input val notation: Any) : BundleInstalled {

    private val aem = target.aem

    private val resolvedFile by lazy { aem.common.resolveFile(notation) }

    override val file = aem.obj.file { fileProvider(aem.obj.provider { resolvedFile }) }

    override val dirPath = aem.obj.string { convention(target.bundlePath) }

    override val fileName = aem.obj.string { convention(aem.obj.provider { resolvedFile.name }) }

    override val vaultFilter = aem.obj.boolean { convention(target.vaultFilters) }

    override val vaultFilterType = aem.obj.typed<FilterType> { convention(FilterType.FILE) }

    override val runMode = aem.obj.string()

    override val startLevel = aem.obj.int()
}
