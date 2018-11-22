package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemPlugin
import com.cognifide.gradle.aem.base.BasePlugin
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
        plugins.apply(BasePlugin::class.java)
    }

    private fun Project.setupTasks() {
        registerTask(Resolve.NAME, Resolve::class.java) {
            it.mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
        }
        registerTask(Down.NAME, Down::class.java)
        registerTask(Up.NAME, Up::class.java) {
            it.dependsOn(Create.NAME).mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME, Down.NAME)
        }
        registerTask(Restart.NAME, Restart::class.java) {
            it.dependsOn(Down.NAME, Up.NAME)
        }
        registerTask(Create.NAME, Create::class.java) {
            it.dependsOn(Resolve.NAME).mustRunAfter(LifecycleBasePlugin.CLEAN_TASK_NAME)
        }
        registerTask(Destroy.NAME, Destroy::class.java) {
            it.dependsOn(Down.NAME)
        }
        registerTask(Satisfy.NAME, Satisfy::class.java) {
            it.dependsOn(Resolve.NAME).mustRunAfter(Create.NAME, Up.NAME)
        }
        registerTask(Reload.NAME, Reload::class.java) { task ->
            task.mustRunAfter(Satisfy.NAME)
            plugins.withId(PackagePlugin.ID) { task.mustRunAfter(Deploy.NAME) }
        }
        registerTask(Await.NAME, Await::class.java) { task ->
            task.mustRunAfter(Create.NAME, Up.NAME, Satisfy.NAME)
            plugins.withId(PackagePlugin.ID) { task.mustRunAfter(Deploy.NAME) }
        }
        registerTask(Collect.NAME, Collect::class.java) { task ->
            task.mustRunAfter(Satisfy.NAME)
        }
        registerTask(Setup.NAME, Setup::class.java) { task ->
            task.dependsOn(Create.NAME, Up.NAME, Satisfy.NAME).mustRunAfter(Destroy.NAME)
            plugins.withId(PackagePlugin.ID) { task.dependsOn(Deploy.NAME) }
        }
        registerTask(Resetup.NAME, Resetup::class.java) {
            it.dependsOn(Destroy.NAME, Setup.NAME)
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.instance"

        const val FILES_PATH = "local-instance"
    }
}