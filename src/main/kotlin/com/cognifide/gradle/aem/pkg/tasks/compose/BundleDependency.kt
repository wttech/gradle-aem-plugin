package com.cognifide.gradle.aem.pkg.tasks.compose

import com.cognifide.gradle.aem.AemExtension
import java.io.File
import org.gradle.api.artifacts.Configuration

class BundleDependency(
    aem: AemExtension,
    notation: Any,
    var installPath: String,
    var vaultFilter: Boolean
) {

    private val dependency = aem.project.dependencies.create(notation)

    internal val configuration: Configuration = aem.project.configurations.detachedConfiguration(dependency)

    val file: File
        get() = configuration.resolve().first()
}
