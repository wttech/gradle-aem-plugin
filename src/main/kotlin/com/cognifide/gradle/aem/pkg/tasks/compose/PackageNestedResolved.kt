package com.cognifide.gradle.aem.pkg.tasks.compose

import com.cognifide.gradle.aem.pkg.tasks.PackageCompose
import com.cognifide.gradle.common.build.DependencyOptions
import org.gradle.api.tasks.Input

class PackageNestedResolved(private val target: PackageCompose, @Input val notation: Any) : PackageNested {

    private val aem = target.aem

    private val project = aem.project

    override val file = aem.obj.file { fileProvider(aem.obj.provider {
        DependencyOptions.resolveFile(project, DependencyOptions.hintNotation(notation, NOTATION_EXTENSION)) })
    }

    override val dirPath = aem.obj.string { convention(target.nestedPath) } // TODO append group extracted from notation

    override val fileName = aem.obj.string { convention(aem.obj.provider {
        DependencyOptions.determineFileName(project, DependencyOptions.hintNotation(notation, NOTATION_EXTENSION)) }) // TODO impl determineFileName
    }

    override val vaultFilter = aem.obj.boolean { convention(target.vaultFilters) }

    companion object {
        const val NOTATION_EXTENSION = "zip"
    }
}
