package com.cognifide.gradle.aem.bundle.tasks

import com.cognifide.gradle.aem.common.instance.checkAvailable
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.BundleTask
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class BundleInstall : BundleTask() {

    /**
     * Controls if bundle after installation should be immediatelly started.
     */
    @Input
    val start = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("bundle.install.start")?.let { set(it) }
    }

    /**
     * OSGi start level at which installed bundle will be started.
     */
    @Input
    val startLevel = aem.obj.int {
        convention(20)
        aem.prop.int("bundle.install.startLevel")?.let { set(it) }
    }

    /**
     * Controls if bundle dependent packages should be refreshed within installation.
     */
    @Input
    val refreshPackages = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("bundle.install.refreshPackages")?.let { set(it) }
    }

    /**
     * Repeat install when failed (brute-forcing).
     */
    @Internal
    var retry = common.retry { afterSquaredSecond(aem.prop.long("bundle.install.retry") ?: 2) }

    @TaskAction
    open fun install() {
        instances.get().checkAvailable()
        sync { osgiFramework.installBundle(it, start.get(), startLevel.get(), refreshPackages.get(), retry) }
        common.notifier.notify("Bundle installed", "${files.files.fileNames} on ${instances.get().names}")
    }

    init {
        description = "Installs OSGi bundle on instance(s)."
        aem.prop.boolean("bundle.install.awaited")?.let { awaited.set(it) }
    }

    companion object {
        const val NAME = "bundleInstall"
    }
}
