package com.cognifide.gradle.aem.bundle.tasks

import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.Bundle
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class BundleInstall : Bundle() {

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
    var retry = common.retry { afterSquaredSecond(aem.prop.long("bundle.install.retry") ?: 3) }

    @TaskAction
    override fun doSync() {
        super.doSync()
        common.notifier.notify("Bundle installed", "${files.fileNames} on ${instances.names}")
    }

    init {
        description = "Installs OSGi bundle on instance(s)."
        sync.action { osgi.installBundle(it, start.get(), startLevel.get(), refreshPackages.get(), retry) }
        aem.prop.boolean("bundle.install.awaited")?.let { sync.awaited.set(it) }
    }

    companion object {
        const val NAME = "bundleInstall"
    }
}
