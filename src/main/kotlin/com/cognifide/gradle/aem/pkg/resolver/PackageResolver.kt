package com.cognifide.gradle.aem.pkg.resolver

import aQute.bnd.osgi.Jar
import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.file.resolver.Resolver
import java.io.File
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

class PackageResolver(aem: AemExtension, downloadDir: File) : Resolver<PackageGroup>(aem, downloadDir) {

    /**
     * Determines a path in JCR repository in which automatic wrapped bundles will be deployed.
     */
    @Input
    var bundlePath: String = "/apps/gradle-aem-plugin/wrap/install"

    /**
     * A hook which could be used to override default properties used to generate a CRX package from OSGi bundle.
     */
    @Internal
    var bundleProperties: (Jar) -> Map<String, Any> = { mapOf() }

    override fun createGroup(name: String): PackageGroup {
        return PackageGroup(this, name)
    }
}