package com.cognifide.gradle.aem.instance.satisfy

import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.common.file.resolver.FileGroup
import com.cognifide.gradle.common.file.resolver.FileResolution
import com.cognifide.gradle.common.file.resolver.Resolver
import java.io.File
import org.gradle.api.tasks.Input

/**
 * Allows to customize behavior of satisfy task for concrete group of packages.
 */
@Suppress("unchecked_cast")
class PackageGroup(val packageResolver: PackageResolver, name: String) : FileGroup(packageResolver as Resolver<FileGroup>, name) {

    private val aem = packageResolver.aem

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
     * Enables deployment via CRX package activation from author to publishers when e.g they are not accessible.
     */
    @Input
    var distributed: Boolean? = null

    internal var initializer: InstanceSync.() -> Unit = {}

    /**
     * Hook for preparing instance before deploying packages.
     * Customize here options related with: HTTP client (timeouts), package manager (workflows to be toggled) etc.
     */
    fun initializer(callback: InstanceSync.() -> Unit) {
        this.initializer = callback
    }

    internal var finalizer: InstanceSync.() -> Unit = {}

    /**
     * Hook for cleaning instance after deploying packages.
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
