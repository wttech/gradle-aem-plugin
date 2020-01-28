package com.cognifide.gradle.aem.instance.satisfy

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.bundle.BundleFile
import com.cognifide.gradle.aem.common.pkg.PackageDefinition
import com.cognifide.gradle.common.file.resolver.Resolver
import java.io.File
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

class PackageResolver(val aem: AemExtension, downloadDir: File) : Resolver<PackageGroup>(aem.common, downloadDir) {

    /**
     * Determines a path in JCR repository in which automatically wrapped bundles will be deployed.
     */
    @Input
    var bundlePath: String = aem.prop.string("package.resolver.bundlePath") ?: "/apps/gap/wrap/install"

    /**
     * A hook which could be used to override default properties used to generate a CRX package from OSGi bundle.
     */
    @Internal
    var bundleDefinition: PackageDefinition.(BundleFile) -> Unit = {}

    override fun createGroup(name: String): PackageGroup {
        return PackageGroup(this, name)
    }
}
