package com.cognifide.gradle.sling.pkg.tasks.compose

import com.cognifide.gradle.sling.common.pkg.vault.FilterType
import com.cognifide.gradle.sling.pkg.tasks.PackageCompose
import org.gradle.api.tasks.Input

class BundleInstalledResolved(private val target: PackageCompose, @Input val notation: Any) : BundleInstalled {

    private val sling = target.sling

    private val resolvedFile by lazy { sling.common.resolveFile(notation) }

    override val file = sling.obj.file { fileProvider(sling.obj.provider { resolvedFile }) }

    override val dirPath = sling.obj.string { convention(target.bundlePath) }

    override val fileName = sling.obj.string { convention(sling.obj.provider { resolvedFile.name }) }

    override val vaultFilter = sling.obj.boolean { convention(target.vaultFilters) }

    override val vaultFilterType = sling.obj.typed<FilterType> { convention(FilterType.FILE) }

    override val runMode = sling.obj.string()
}
