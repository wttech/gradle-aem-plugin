package com.cognifide.gradle.aem.base

import com.cognifide.gradle.aem.api.AemExtension
import com.cognifide.gradle.aem.base.debug.DebugTask
import com.cognifide.gradle.aem.base.vlt.CheckoutTask
import com.cognifide.gradle.aem.base.vlt.CleanTask
import com.cognifide.gradle.aem.base.vlt.SyncTask
import com.cognifide.gradle.aem.base.vlt.VltTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * Provides configuration used by both package and instance plugins.
 */
class BasePlugin : Plugin<Project> {

    companion object {
        val PKG = "com.cognifide.gradle.aem"

        val ID = "com.cognifide.aem.base"
    }

    override fun apply(project: Project) {
        setupDependentPlugins(project)
        setupExtensions(project)
        setupTasks(project)
    }

    private fun setupDependentPlugins(project: Project) {
        project.plugins.apply(BasePlugin::class.java)
    }

    private fun setupExtensions(project: Project) {
        project.extensions.create(AemExtension.NAME, AemExtension::class.java, project)
    }

    private fun setupTasks(project: Project) {
        project.tasks.create(DebugTask.NAME, DebugTask::class.java)

        val clean = project.tasks.create(CleanTask.NAME, CleanTask::class.java)
        val vlt = project.tasks.create(VltTask.NAME, VltTask::class.java)
        val checkout = project.tasks.create(CheckoutTask.NAME, CheckoutTask::class.java)
        val sync = project.tasks.create(SyncTask.NAME, SyncTask::class.java)

        val baseClean = project.tasks.getByName(LifecycleBasePlugin.CLEAN_TASK_NAME)

        clean.mustRunAfter(baseClean)
        vlt.mustRunAfter(baseClean)
        checkout.mustRunAfter(baseClean)
        sync.mustRunAfter(baseClean)
    }

}