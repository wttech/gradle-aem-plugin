package com.cognifide.gradle.aem.instance.satisfy

import com.cognifide.gradle.aem.common.file.resolver.FileGroup
import com.cognifide.gradle.aem.common.file.resolver.FileResolution
import com.cognifide.gradle.aem.common.instance.InstanceSync
import java.io.File
import org.gradle.api.tasks.Input

class PackageGroup(val resolver: PackageResolver, name: String) : FileGroup(resolver.aem, resolver.downloadDir, name) {

    /**
     * Forces to upload and install package again regardless its state on instances (already uploaded / installed).
     */
    @Input
    var greedy = false

    /**
     * Instance name filter for excluding group from deployment.
     */
    var instanceName = "*"

    internal var initializer: InstanceSync.() -> Unit = {}

    /**
     * Hook for preparing instance before deploying packages
     */
    fun initializer(callback: InstanceSync.() -> Unit) {
        this.initializer = callback
    }

    internal var finalizer: InstanceSync.() -> Unit = {}

    /**
     * Hook for cleaning instance after deploying packages
     */
    fun finalizer(callback: InstanceSync.() -> Unit) {
        this.finalizer = callback
    }

    internal var completer: () -> Unit = { aem.instanceActions.awaitUp() }

    /**
     * Hook after deploying all packages to all instances called only when
     * at least one package was deployed on any instance.
     */
    fun completer(callback: () -> Unit) {
        this.completer = callback
    }

    override fun createResolution(id: String, resolver: (FileResolution) -> File): FileResolution {
        return PackageResolution(this, id, resolver)
    }
}
