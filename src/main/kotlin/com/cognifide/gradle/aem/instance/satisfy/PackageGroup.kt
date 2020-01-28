package com.cognifide.gradle.aem.instance.satisfy

import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.common.build.Retry
import com.cognifide.gradle.common.file.resolver.FileGroup
import com.cognifide.gradle.common.file.resolver.FileResolution
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

/**
 * Allows to customize behavior of satisfy task for concrete group of packages.
 */
class PackageGroup(val resolver: PackageResolver, name: String) : FileGroup(resolver.common, resolver.downloadDir, name) {

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
     * Enables deployment via CRX package activation from author to publishers when e.g they are not accessible.
     */
    @Input
    var distributed: Boolean? = null

    /**
     * Force upload CRX package regardless if it was previously uploaded.
     */
    @Input
    var uploadForce: Boolean? = null

    /**
     * Repeat upload when failed (brute-forcing).
     */
    @Internal
    @get:JsonIgnore
    var uploadRetry: Retry? = null

    /**
     * Repeat install when failed (brute-forcing).
     */
    @Internal
    @get:JsonIgnore
    var installRetry: Retry? = null

    /**
     * Determines if when on package install, sub-packages included in CRX package content should be also installed.
     */
    @Input
    var installRecursive: Boolean? = null

    /**
     * Allows to temporarily enable or disable workflows during CRX package deployment.
     */
    @Input
    var workflowToggle = mutableMapOf<String, Boolean>()

    /**
     * Allows to temporarily enable or disable workflow during CRX package deployment.
     */
    fun workflowToggle(id: String, flag: Boolean) {
        workflowToggle[id] = flag
    }

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
