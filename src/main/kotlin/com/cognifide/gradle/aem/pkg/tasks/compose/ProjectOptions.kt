package com.cognifide.gradle.aem.pkg.tasks.compose

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.pkg.tasks.PackageCompose
import org.gradle.api.tasks.bundling.Jar

/**
 * Allows to customize default conventions related with specific project from which CRX package is being composed.
 */
class ProjectOptions {

    /**
     * Controls if JCR content from particular project should be taken.
     */
    var composeContent: Boolean = true

    /**
     * Determines compose task(s) in particular project to be considered when composing merged CRX package.
     */
    var composeTasks: AemExtension.() -> Collection<PackageCompose> = { tasks.getAll(PackageCompose::class.java) }

    /**
     * Controls if Vault hooks from particular project should be taken.
     */
    var vaultHooks: Boolean = true

    /**
     * Controls if Vault filters from particular project should be taken.
     */
    var vaultFilters: Boolean = true

    /**
     * Controls if Vault Node Types from particular project should be taken.
     */
    var vaultNodeTypes: Boolean = true

    /**
     * Determines if OSGi bundle built in particular project should be taken.
     */
    var bundleBuilt: Boolean = true

    /**
     * Determines JAR task(s) in particular project to be considered when composing merged CRX package.
     */
    var bundleTasks: AemExtension.() -> Collection<Jar> = { tasks.getAll(Jar::class.java) }

    /**
     * Controls if extra OSGi bundles from particular project should be taken.
     */
    var bundleDependent: Boolean = true

    /**
     * JCR content path of extra OSGi bundles taken from particular project.
     */
    var bundlePath: String? = null

    /**
     * Determines where extra OSGi bundles will be placed.
     */
    var bundleRunMode: String? = null

    /**
     * Controls if Vault filter should be automatically generated for extra OSGi bundles.
     */
    var bundleVaultFilter: Boolean = true

    /**
     * Controls if nested CRX sub-packages from particular project should be taken.
     */
    var packageDependent: Boolean = true

    /**
     * JCR content path of nested CRX sub-packages taken from particular project.
     */
    var packagePath: String? = null

    /**
     * Controls if Vault filter should be automatically generated for CRX sub-packages.
     */
    var packageVaultFilter: Boolean = true

    internal fun bundlePath(otherBundlePath: String, otherBundleRunMode: String?): String {
        val effectiveBundlePath = bundlePath ?: otherBundlePath
        val effectiveBundleRunMode = bundleRunMode ?: otherBundleRunMode

        var result = effectiveBundlePath
        if (!effectiveBundleRunMode.isNullOrBlank()) {
            result = "$effectiveBundlePath.$effectiveBundleRunMode"
        }

        return result
    }

    internal fun packagePath(otherPackagePath: String): String {
        return packagePath ?: otherPackagePath
    }
}