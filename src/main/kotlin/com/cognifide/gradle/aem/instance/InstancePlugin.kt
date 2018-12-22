package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.AemPlugin
import com.cognifide.gradle.aem.config.ConfigPlugin
import com.cognifide.gradle.aem.instance.tasks.*
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.pkg.tasks.Deploy
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * Separate plugin which provides tasks for managing local instances.
 * Most often should be applied only to one project in build.
 * Applying it multiple times to same configuration could case confusing errors like AEM started multiple times.
 */
class InstancePlugin : AemPlugin() {

    override fun Project.configure() {
        setupDependentPlugins()
        setupTasks()
    }

    private fun Project.setupDependentPlugins() {
        plugins.apply(ConfigPlugin::class.java)
    }

    private fun Project.setupTasks() {
        with(AemExtension.of(this).tasks) {
            register(Resolve.NAME, Resolve::class.java) {
                it.mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
            }
            register(Down.NAME, Down::class.java)
            register(Up.NAME, Up::class.java) {
                it.dependsOn(Create.NAME).mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME, Down.NAME)
            }
            register(Restart.NAME, Restart::class.java) {
                it.dependsOn(Down.NAME, Up.NAME)
            }
            register(Create.NAME, Create::class.java) {
                it.dependsOn(Resolve.NAME).mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
            }
            register(Destroy.NAME, Destroy::class.java) {
                it.dependsOn(Down.NAME)
            }
            register(Satisfy.NAME, Satisfy::class.java) {
                it.dependsOn(Resolve.NAME).mustRunAfter(Create.NAME, Up.NAME)
            }
            register(Reload.NAME, Reload::class.java) { task ->
                task.mustRunAfter(Satisfy.NAME)
                plugins.withId(PackagePlugin.ID) { task.mustRunAfter(Deploy.NAME) }
            }
            register(Await.NAME, Await::class.java) { task ->
                task.mustRunAfter(Create.NAME, Up.NAME, Satisfy.NAME)
                plugins.withId(PackagePlugin.ID) { task.mustRunAfter(Deploy.NAME) }
            }
            register(Collect.NAME, Collect::class.java) { task ->
                task.mustRunAfter(Satisfy.NAME)
            }
            register(Setup.NAME, Setup::class.java) { task ->
                task.dependsOn(Create.NAME, Up.NAME, Satisfy.NAME).mustRunAfter(Destroy.NAME)
                plugins.withId(PackagePlugin.ID) { task.dependsOn(Deploy.NAME) }
            }
            register(Resetup.NAME, Resetup::class.java) {
                it.dependsOn(Destroy.NAME, Setup.NAME)
            }
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.instance"

        const val FILES_PATH = "local-instance"
    }
}