package com.cognifide.gradle.aem.bundle.tasks

import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import com.cognifide.gradle.aem.common.instance.checkAvailable
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.BundleTask
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class BundleInstall : BundleTask() {

    /**
     * Check instance(s) health after installing bundles.
     */
    @Input
    var awaited: Boolean = aem.prop.boolean("bundle.install.awaited") ?: true

    /**
     * Controls if bundle after installation should be immediatelly started.
     */
    @Input
    var start: Boolean = aem.prop.boolean("bundle.install.start") ?: true

    /**
     * OSGi start level at which installed bundle will be started.
     */
    @Input
    var startLevel: Int = aem.prop.int("bundle.install.startLevel") ?: 20

    /**
     * Controls if bundle dependent packages should be refreshed within installation.
     */
    @Input
    var refreshPackages: Boolean = aem.prop.boolean("bundle.install.refreshPackages") ?: true

    /**
     * Repeat install when failed (brute-forcing).
     */
    @Internal
    var retry = common.retry { afterSquaredSecond(aem.prop.long("bundle.install.retry") ?: 2) }

    /**
     * Hook for preparing instance before installing bundles
     */
    @Internal
    var initializer: InstanceSync.() -> Unit = {}

    /**
     * Hook for cleaning instance after installing bundles
     */
    @Internal
    var finalizer: InstanceSync.() -> Unit = {}

    /**
     * Hook after installing all bundles to all instances.
     */
    @Internal
    var completer: () -> Unit = { awaitUp() }

    private var awaitUpOptions: AwaitUpAction.() -> Unit = {}

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
    open fun install() {
        instances.checkAvailable()

        common.progress(instances.size * bundles.size) {
            aem.syncFiles(instances, bundles) { pkg ->
                increment("Installing bundle '${pkg.name}' to instance '${instance.name}'") {
                    initializer()
                    osgiFramework.installBundle(pkg, start, startLevel, refreshPackages, retry)
                    finalizer()
                }
            }
        }

        completer()

        common.notifier.notify("Bundle installed", "${bundles.fileNames} on ${instances.names}")
    }

    init {
        description = "Installs OSGi bundle on instance(s)."
    }

    companion object {
        const val NAME = "bundleInstall"
    }
}
