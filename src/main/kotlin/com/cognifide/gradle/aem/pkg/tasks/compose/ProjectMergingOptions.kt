package com.cognifide.gradle.aem.pkg.tasks.compose

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.bundle.tasks.BundleCompose
import com.cognifide.gradle.aem.pkg.tasks.PackageCompose
import org.gradle.api.Task

/**
 * Allows to override project specific options while composing merged CRX package.
 */
class ProjectMergingOptions {

    /**
     * Controls if JCR content from particular project should be taken.
     */
    var composeContent: Boolean = true

    /**
     * Determines compose task(s) in particular project to be considered when composing merged CRX package.
     */
    var composeTasks: AemExtension.() -> Collection<PackageCompose> = { common.tasks.getAll(PackageCompose::class.java) }

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
     * Controls if Vault properties (e.g hooks related) from particular project should be taken.
     */
    var vaultProperties: Boolean = true

    /**
     * Determines if OSGi bundle built in particular project should be taken.
     */
    var bundleBuilt: Boolean = true

    /**
     * Determines JAR task(s) in particular project to be considered when composing merged CRX package.
     */
    var bundleTasks: AemExtension.() -> Collection<BundleCompose> = { common.tasks.getAll(BundleCompose::class.java) }

    /**
     * Determines extra task(s) in particular project to be executed when composing merged CRX package.
     */
    var extraTasks: AemExtension.() -> Collection<Task> = { common.tasks.checks }

    /**
     * Controls if extra OSGi bundles from particular project should be taken.
     */
    var bundleDependent: Boolean = true

    /**
     * Controls if nested CRX sub-packages from particular project should be taken.
     */
    var packageDependent: Boolean = true
}
