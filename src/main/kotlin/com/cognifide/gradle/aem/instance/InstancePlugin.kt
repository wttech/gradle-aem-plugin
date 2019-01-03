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
                mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
            }
            register(Down.NAME, Down::class.java)
            register(Up.NAME, Up::class.java) {
                dependsOn(Create.NAME).mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME, Down.NAME)
            }
            register(Restart.NAME, Restart::class.java) {
                dependsOn(Down.NAME, Up.NAME)
            }
            register(Create.NAME, Create::class.java) {
                dependsOn(Resolve.NAME).mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
            }
            register(Destroy.NAME, Destroy::class.java) {
                dependsOn(Down.NAME)
            }
            register(Satisfy.NAME, Satisfy::class.java) {
                dependsOn(Resolve.NAME).mustRunAfter(Create.NAME, Up.NAME)
            }
            register(Reload.NAME, Reload::class.java) {
                mustRunAfter(Satisfy.NAME)
                plugins.withId(PackagePlugin.ID) { mustRunAfter(Deploy.NAME) }
            }
            register(Await.NAME, Await::class.java) {
                mustRunAfter(Create.NAME, Up.NAME, Satisfy.NAME)
                plugins.withId(PackagePlugin.ID) { mustRunAfter(Deploy.NAME) }
            }
            register(Collect.NAME, Collect::class.java) {
                mustRunAfter(Satisfy.NAME)
            }
            register(Setup.NAME, Setup::class.java) {
                dependsOn(Create.NAME, Up.NAME, Satisfy.NAME).mustRunAfter(Destroy.NAME)
                plugins.withId(PackagePlugin.ID) { dependsOn(Deploy.NAME) }
            }
            register(Resetup.NAME, Resetup::class.java) {
                dependsOn(Destroy.NAME, Setup.NAME)
            }
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.instance"

        const val FILES_PATH = "local-instance"
    }
}