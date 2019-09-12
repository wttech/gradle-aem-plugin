package com.cognifide.gradle.aem.instance.satisfy

import aQute.bnd.osgi.Jar
import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.resolver.Resolver
import com.cognifide.gradle.aem.common.pkg.PackageDefinition
import java.io.File
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

class PackageResolver(aem: AemExtension, downloadDir: File) : Resolver<PackageGroup>(aem, downloadDir) {

    /**
     * Determines a path in JCR repository in which automatically wrapped bundles will be deployed.
     */
    @Input
    var bundlePath: String = aem.props.string("package.resolver.bundlePath") ?: "/apps/gap/wrap/install"

    /**
     * A hook which could be used to override default properties used to generate a CRX package from OSGi bundle.
     */
    @Internal
    var bundleDefinition: PackageDefinition.(Jar) -> Unit = {}

    override fun createGroup(name: String): PackageGroup {
        return PackageGroup(this, name)
    }
}
