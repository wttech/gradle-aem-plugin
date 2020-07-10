package com.cognifide.gradle.sling.pkg

import com.cognifide.gradle.sling.common.CommonPlugin
import com.cognifide.gradle.sling.pkg.tasks.PackageConfig
import com.cognifide.gradle.sling.pkg.tasks.PackageSync
import com.cognifide.gradle.sling.pkg.tasks.PackageVlt
import com.cognifide.gradle.common.CommonDefaultPlugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * Provides tasks useful for synchronizing JCR content from running Sling instance into built CRX package.
 */
class PackageSyncPlugin : CommonDefaultPlugin() {

    override fun Project.configureProject() {
        setupDependentPlugins()
        setupTasks()
    }

    private fun Project.setupDependentPlugins() {
        plugins.apply(CommonPlugin::class.java)
    }

    private fun Project.setupTasks() = tasks {
        val clean = named<Task>(LifecycleBasePlugin.CLEAN_TASK_NAME)

        register<PackageVlt>(PackageVlt.NAME)
        register<PackageSync>(PackageSync.NAME) {
            mustRunAfter(clean)
        }
        register<PackageConfig>(PackageConfig.NAME)
    }

    companion object {
        const val ID = "com.cognifide.sling.package.sync"
    }
}
