package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.internal.FileResolver
import groovy.lang.Closure
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.util.ConfigureUtil

open class CreateTask : SyncTask() {

    companion object {
        val NAME = "aemCreate"

        val DOWNLOAD_DIR = "download"

        val LICENSE_URL_PROP = "aem.deploy.instance.local.jarUrl"

        val JAR_URL_PROP = "aem.deploy.instance.local.licenseUrl"
    }

    @Internal
    val instanceFileResolver = FileResolver(project, AemTask.temporaryDir(project, NAME, DOWNLOAD_DIR))

    init {
        group = AemTask.GROUP
        description = "Creates local AEM instance(s)."

        instanceFileResolver.attach(this)
        instanceFileFromProperties()
        project.afterEvaluate {
            outputs.dir(config.instancesPath)
        }
    }

    private fun instanceFileFromProperties() {
        val jarUrl = project.properties[JAR_URL_PROP] as String?
                ?: project.extensions.extraProperties[JAR_URL_PROP] as String?
        if (!jarUrl.isNullOrBlank()) {
            instanceFileResolver.url(jarUrl!!)
        }

        val licenseUrl = project.properties[LICENSE_URL_PROP] as String?
                ?: project.extensions.extraProperties[LICENSE_URL_PROP] as String?
        if (!licenseUrl.isNullOrBlank()) {
            instanceFileResolver.url(licenseUrl!!)
        }
    }

    fun instanceFiles(closure: Closure<*>) {
        ConfigureUtil.configure(closure, instanceFileResolver)
    }

    @TaskAction
    fun create() {
        logger.info("Resolving instance files")
        val files = instanceFileResolver.resolveFiles()

        logger.info("Creating instances")
        synchronizeLocalInstances({ it.create(files) })
    }

}