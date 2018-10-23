package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.base.BasePlugin
import com.cognifide.gradle.aem.pkg.deploy.ActivateTask
import com.cognifide.gradle.aem.pkg.deploy.DeleteTask
import com.cognifide.gradle.aem.pkg.deploy.DeployTask
import com.cognifide.gradle.aem.pkg.deploy.PurgeTask
import com.cognifide.gradle.aem.pkg.deploy.UninstallTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

class PackagePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        with(project, {
            setupDependentPlugins()
            setupTasks()
        })
    }

    private fun Project.setupDependentPlugins() {
        plugins.apply(BasePlugin::class.java)
    }

    private fun Project.setupTasks() {
        val clean = tasks.getByName(LifecycleBasePlugin.CLEAN_TASK_NAME)

        val prepare = tasks.create(PrepareTask.NAME, PrepareTask::class.java)
        val compose = tasks.create(ComposeTask.NAME, ComposeTask::class.java)
        tasks.create(DeleteTask.NAME, DeleteTask::class.java)
        tasks.create(PurgeTask.NAME, PurgeTask::class.java)
        tasks.create(UninstallTask.NAME, UninstallTask::class.java)
        tasks.create(ActivateTask.NAME, ActivateTask::class.java)
        val deploy = tasks.create(DeployTask.NAME, DeployTask::class.java)

        val assemble = tasks.getByName(LifecycleBasePlugin.ASSEMBLE_TASK_NAME)
        val check = tasks.getByName(LifecycleBasePlugin.CHECK_TASK_NAME)
        val build = tasks.getByName(LifecycleBasePlugin.BUILD_TASK_NAME)


        assemble.mustRunAfter(clean)
        check.mustRunAfter(clean)
        build.dependsOn(compose)

        prepare.mustRunAfter(clean)

        compose.dependsOn(prepare, assemble, check)
        compose.mustRunAfter(clean)

        deploy.dependsOn(compose)
    }

    companion object {
        const val ID = "com.cognifide.aem.package"

        const val VLT_PATH = "META-INF/vault"

        const val VLT_PROPERTIES = "$VLT_PATH/properties.xml"

        const val JCR_ROOT = "jcr_root"
    }

}