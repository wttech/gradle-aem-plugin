package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.base.BasePlugin
import com.cognifide.gradle.aem.pkg.deploy.*
import com.cognifide.gradle.aem.bundle.BundleTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.language.base.plugins.LifecycleBasePlugin

class PackagePlugin : Plugin<Project> {

    companion object {
        val ID = "com.cognifide.aem.package"

        val VLT_PATH = "META-INF/vault"

        val VLT_PROPERTIES = "$VLT_PATH/properties.xml"

        val JCR_ROOT = "jcr_root"
    }

    override fun apply(project: Project) {
        setupDependentPlugins(project)
        setupTasks(project)
    }

    private fun setupDependentPlugins(project: Project) {
        project.plugins.apply(BasePlugin::class.java)
    }

    private fun setupTasks(project: Project) {
        val clean = project.tasks.getByName(LifecycleBasePlugin.CLEAN_TASK_NAME)

        val prepare = project.tasks.create(PrepareTask.NAME, PrepareTask::class.java)
        val compose = project.tasks.create(ComposeTask.NAME, ComposeTask::class.java)
        val upload = project.tasks.create(UploadTask.NAME, UploadTask::class.java)
        project.tasks.create(DeleteTask.NAME, DeleteTask::class.java)
        project.tasks.create(PurgeTask.NAME, PurgeTask::class.java)
        val install = project.tasks.create(InstallTask.NAME, InstallTask::class.java)
        project.tasks.create(UninstallTask.NAME, UninstallTask::class.java)
        val activate = project.tasks.create(ActivateTask.NAME, ActivateTask::class.java)
        val deploy = project.tasks.create(DeployTask.NAME, DeployTask::class.java)

        val assemble = project.tasks.getByName(LifecycleBasePlugin.ASSEMBLE_TASK_NAME)
        val check = project.tasks.getByName(LifecycleBasePlugin.CHECK_TASK_NAME)
        val build = project.tasks.getByName(LifecycleBasePlugin.BUILD_TASK_NAME)

        assemble.mustRunAfter(clean)
        check.mustRunAfter(clean)
        build.dependsOn(compose)

        prepare.mustRunAfter(clean)

        compose.dependsOn(prepare, assemble, check)
        compose.mustRunAfter(clean)

        upload.dependsOn(compose)
        install.mustRunAfter(compose, upload)
        activate.mustRunAfter(compose, upload, install)

        deploy.dependsOn(compose)
    }

}