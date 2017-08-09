package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.deploy.SyncTask
import com.cognifide.gradle.aem.internal.FileResolver
import com.cognifide.gradle.aem.internal.PropertyParser
import groovy.lang.Closure
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.util.ConfigureUtil

open class SatisfyTask : SyncTask() {

    companion object {
        val NAME = "aemSatisfy"

        val DOWNLOAD_DIR = "download"
    }

    @Internal
    val packageProvider = FileResolver(project, AemTask.temporaryDir(project, NAME, DOWNLOAD_DIR))

    init {
        group = AemTask.GROUP
        description = "Satisfies AEM by uploading & installing dependent packages on instance(s)."
    }

    fun packages(closure: Closure<*>) {
        ConfigureUtil.configure(closure, packageProvider)
    }

    @TaskAction
    fun satisfy() {
        logger.info("Providing packages from local and remote sources.")

        val groupedFiles = packageProvider.groupedFiles({ fileGroup ->
            PropertyParser(project).filter(fileGroup, "aem.deploy.satisfy.group")
        })

        logger.info("Packages provided (${groupedFiles.map { it.value.size }.sum()})")

        for ((group, files) in groupedFiles) {
            logger.info("Satisfying group of packages '$group'")

            synchronizeInstances({ sync -> files.onEach { sync.deployPackage(it) } })
            awaitStableInstances()
        }
    }

}