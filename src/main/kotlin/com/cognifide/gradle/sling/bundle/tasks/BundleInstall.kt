package com.cognifide.gradle.sling.bundle.tasks

import com.cognifide.gradle.sling.common.instance.names
import com.cognifide.gradle.sling.common.tasks.BundleTask
import com.cognifide.gradle.sling.common.utils.fileNames
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class BundleInstall : BundleTask() {

    /**
     * Controls if bundle after installation should be immediatelly started.
     */
    @Input
    val start = sling.obj.boolean {
        convention(true)
        sling.prop.boolean("bundle.install.start")?.let { set(it) }
    }

    /**
     * OSGi start level at which installed bundle will be started.
     */
    @Input
    val startLevel = sling.obj.int {
        convention(20)
        sling.prop.int("bundle.install.startLevel")?.let { set(it) }
    }

    /**
     * Controls if bundle dependent packages should be refreshed within installation.
     */
    @Input
    val refreshPackages = sling.obj.boolean {
        convention(true)
        sling.prop.boolean("bundle.install.refreshPackages")?.let { set(it) }
    }

    /**
     * Repeat install when failed (brute-forcing).
     */
    @Internal
    var retry = common.retry { afterSquaredSecond(sling.prop.long("bundle.install.retry") ?: 2) }

    @TaskAction
    open fun install() {
        sync { osgiFramework.installBundle(it, start.get(), startLevel.get(), refreshPackages.get(), retry) }
        common.notifier.notify("Bundle installed", "${files.files.fileNames} on ${instances.get().names}")
    }

    init {
        description = "Installs OSGi bundle on instance(s)."
        sling.prop.boolean("bundle.install.awaited")?.let { awaited.set(it) }
    }

    companion object {
        const val NAME = "bundleInstall"
    }
}
