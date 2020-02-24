package com.cognifide.gradle.aem.instance.satisfy

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.common.file.resolver.FileGroup
import com.cognifide.gradle.common.file.resolver.FileResolution
import com.cognifide.gradle.common.file.resolver.Resolver
import java.io.File

/**
 * Allows to customize behavior of satisfy task for concrete group of packages.
 */
@Suppress("unchecked_cast")
class PackageGroup(val packageResolver: PackageResolver, name: String) : FileGroup(packageResolver as Resolver<FileGroup>, name) {

    private val aem = packageResolver.aem

    /**
     * Instance name filter for excluding group from deployment.
     */
    val instanceName = aem.obj.string { convention("*") }

    /**
     * Forces to upload and install package again regardless its state on instances (already uploaded / installed).
     */
    val greedy = aem.obj.boolean()

    /**
     * Enables deployment via CRX package activation from author to publishers when e.g they are not accessible.
     */
    val distributed = aem.obj.boolean()

    /**
     * Hook for preparing instance before deploying packages.
     * Customize here options related with: HTTP client (timeouts), package manager (workflows to be toggled) etc.
     */
    fun initializer(callback: InstanceSync.() -> Unit) {
        this.initializer = callback
    }

    internal var initializer: InstanceSync.() -> Unit = {}

    /**
     * Hook for cleaning instance after deploying packages.
     */
    fun finalizer(callback: InstanceSync.() -> Unit) {
        this.finalizer = callback
    }

    internal var finalizer: InstanceSync.() -> Unit = {}

    /**
     * Hook after deploying all packages to all instances called only when
     * at least one package was deployed on any instance.
     */
    fun completer(callback: (Collection<Instance>) -> Unit) {
        this.completer = callback
    }

    internal var completer: (Collection<Instance>) -> Unit = { aem.instanceManager.awaitUp(it) }

    override fun createResolution(id: String, resolver: (FileResolution) -> File): FileResolution {
        return PackageResolution(this, id, resolver)
    }
}
