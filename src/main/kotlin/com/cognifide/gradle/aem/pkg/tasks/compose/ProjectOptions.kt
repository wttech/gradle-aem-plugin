package com.cognifide.gradle.aem.pkg.tasks.compose

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.pkg.tasks.PackageCompose
import org.gradle.api.tasks.bundling.Jar

class ProjectOptions {

    /**
     * Determines if JCR content from separate project should be included in composed package.
     */
    var composeContent: Boolean = true

    var composeTasks: AemExtension.() -> Collection<PackageCompose> = { tasks.getAll(PackageCompose::class.java) }

    var vaultHooks: Boolean = true

    var vaultFilters: Boolean = true

    var vaultNodeTypes: Boolean = true

    /**
     * Determines if OSGi bundle built in separate project should be included in composed package.
     */
    var bundleBuilt: Boolean = true

    var bundleTasks: AemExtension.() -> Collection<Jar> = { tasks.getAll(Jar::class.java) }

    var bundleDependent: Boolean = true

    var bundlePath: String? = null

    var bundleRunMode: String? = null

    internal fun bundlePath(otherBundlePath: String, otherBundleRunMode: String?): String {
        val effectiveBundlePath = bundlePath ?: otherBundlePath
        val effectiveBundleRunMode = bundleRunMode ?: otherBundleRunMode

        var result = effectiveBundlePath
        if (!effectiveBundleRunMode.isNullOrBlank()) {
            result = "$effectiveBundlePath.$effectiveBundleRunMode"
        }

        return result
    }
}