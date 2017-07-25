package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemInstance
import com.cognifide.gradle.aem.AemLocalInstance
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
    }

    fun instanceFiles(closure: Closure<*>) {
        ConfigureUtil.configure(closure, instanceFileResolver)
    }

    fun instanceJar() {
        val jarUrl = project.properties[JAR_URL_PROP] as String?
                ?: project.extensions.extraProperties[JAR_URL_PROP] as String?
        if (!jarUrl.isNullOrBlank()) {
            instanceFileResolver.download(jarUrl!!)
        }
    }

    fun instanceLicense() {
        val licenseUrl = project.properties[LICENSE_URL_PROP] as String?
                ?: project.extensions.extraProperties[LICENSE_URL_PROP] as String?
        if (!licenseUrl.isNullOrBlank()) {
            instanceFileResolver.download(licenseUrl!!)
        }
    }

    @TaskAction
    fun create() {
        if (!instanceFileResolver.configured) {
            logger.info("Skipping creation, because no instance files are configured.")
            return
        }

        logger.info("Resolving files")
        val files = instanceFileResolver.resolveFiles()

        synchronize({ sync ->
            val localInstance = AemLocalInstance(sync.instance, project)

            logger.info("Creating: $localInstance")
            localInstance.create(files)
            logger.info("Created: $localInstance")
        }, AemInstance.filter(project, AemInstance.FILTER_LOCAL))
    }

}