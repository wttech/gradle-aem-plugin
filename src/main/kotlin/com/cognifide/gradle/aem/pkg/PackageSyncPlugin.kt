package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.common.CommonPlugin
import com.cognifide.gradle.aem.pkg.tasks.PackageSync
import com.cognifide.gradle.aem.pkg.tasks.PackageVlt
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * Provides tasks useful for synchronizing JCR content from running AEM instance into built CRX package.
 */
class PackageSyncPlugin : AemPlugin() {

    override fun Project.configure() {
        setupDependentPlugins()
        setupTasks()
    }

    private fun Project.setupDependentPlugins() {
        plugins.apply(CommonPlugin::class.java)
    }

    private fun Project.setupTasks() {
        with(AemExtension.of(this).tasks) {
            register<PackageVlt>(PackageVlt.NAME) {
                mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
            }
            register<PackageSync>(PackageSync.NAME) {
                mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
            }
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.package.sync"
    }
}
