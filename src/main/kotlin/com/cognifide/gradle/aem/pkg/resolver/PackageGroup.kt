package com.cognifide.gradle.aem.pkg.resolver

import com.cognifide.gradle.aem.common.file.resolver.FileGroup
import com.cognifide.gradle.aem.common.file.resolver.FileResolution
import com.cognifide.gradle.aem.instance.InstanceSync
import org.gradle.api.tasks.Input
import java.io.File

class PackageGroup(val resolver: PackageResolver, name: String) : FileGroup(resolver.downloadDir, name) {

    private val aem = resolver.aem

    /**
     * Forces to upload and install package again regardless its state on instances (already uploaded / installed).
     */
    @Input
    var greedy = false

    /**
     * Instance name filter for excluding group from deployment.
     */
    var instanceName = "*"

    /**
     * Hook for preparing instance before deploying packages
     */
    var initializer: InstanceSync.() -> Unit = {}

    /**
     * Hook for cleaning instance after deploying packages
     */
    var finalizer: InstanceSync.() -> Unit = {}

    /**
     * Hook after deploying all packages to all instances called only when
     * at least one package was deployed on any instance.
     */
    var completer: () -> Unit = { aem.actions.await() }

    override fun createResolution(id: String, resolver: (FileResolution) -> File): FileResolution {
        return PackageResolution(this, id, resolver)
    }
}
