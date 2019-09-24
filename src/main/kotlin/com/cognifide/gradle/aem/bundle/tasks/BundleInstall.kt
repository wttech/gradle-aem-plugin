package com.cognifide.gradle.aem.bundle.tasks

import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import com.cognifide.gradle.aem.common.instance.checkAvailable
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.BundleTask
import com.cognifide.gradle.aem.common.utils.fileNames
import com.fasterxml.jackson.annotation.JsonIgnore
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class BundleInstall : BundleTask() {

    /**
     * Check instance(s) health after installing bundles.
     */
    @Input
    var awaited: Boolean = aem.props.boolean("bundle.install.awaited") ?: true


    /**
     * Repeat install when failed (brute-forcing).
     */
    @Internal
    @get:JsonIgnore
    var retry = aem.retry { afterSquaredSecond(aem.props.long("bundle.install.retry") ?: 2) }

    /**
     * Hook for preparing instance before installing bundles
     */
    @Internal
    @get:JsonIgnore
    var initializer: InstanceSync.() -> Unit = {}

    /**
     * Hook for cleaning instance after installing bundles
     */
    @Internal
    @get:JsonIgnore
    var finalizer: InstanceSync.() -> Unit = {}

    /**
     * Hook after installing all bundles to all instances.
     */
    @Internal
    @get:JsonIgnore
    var completer: () -> Unit = { awaitUp() }

    private var awaitUpOptions: AwaitUpAction.() -> Unit = {}

    init {
        description = "Installs OSGi bundle on instance(s)."
    }

    /**
     * Controls await up action.
     */
    fun awaitUp(options: AwaitUpAction.() -> Unit) {
        this.awaitUpOptions = options
    }

    fun awaitUp() {
        if (awaited) {
            aem.instanceActions.awaitUp {
                instances = this@BundleInstall.instances
                awaitUpOptions()
            }
        }
    }

    @TaskAction
    open fun deploy() {
        instances.checkAvailable()

        aem.progress(instances.size * bundles.size) {
            aem.syncFiles(instances, bundles) { pkg ->
                increment("Installing bundle '${pkg.name}' to instance '${instance.name}'") {
                    initializer()
                    osgiFramework.installBundle(pkg, retry)
                    finalizer()
                }
            }
        }

        completer()

        aem.notifier.notify("Bundle installed", "${bundles.fileNames} on ${instances.names}")
    }

    companion object {
        const val NAME = "bundleInstall"
    }
}
