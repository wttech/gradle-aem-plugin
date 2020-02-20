package com.cognifide.gradle.aem.pkg.tasks.compose

import com.cognifide.gradle.aem.pkg.tasks.PackageCompose
import com.cognifide.gradle.common.build.DependencyOptions
import org.gradle.api.tasks.Input

class PackageNestedResolved(private val target: PackageCompose,  @Input val notation: Any) : PackageNested {

    private val aem = target.aem

    private val project = aem.project

    override val file = aem.obj.file { fileProvider(aem.obj.provider { DependencyOptions.resolveFile(project, notation) }) }

    override val dirPath = aem.obj.string { convention(target.packagePath) }

    override val fileName = aem.obj.string { convention(aem.obj.provider { DependencyOptions.determineFileName(project, notation) }) }

    override val vaultFilter = aem.obj.boolean { convention(target.vaultFilters) }
}