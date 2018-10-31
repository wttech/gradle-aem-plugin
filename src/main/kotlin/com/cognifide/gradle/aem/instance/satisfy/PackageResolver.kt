package com.cognifide.gradle.aem.instance.satisfy

import aQute.bnd.osgi.Jar
import com.cognifide.gradle.aem.internal.file.resolver.Resolver
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import java.io.File

class PackageResolver(project: Project, downloadDir: File) : Resolver<PackageGroup>(project, downloadDir) {

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