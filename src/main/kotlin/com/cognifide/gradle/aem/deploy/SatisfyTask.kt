package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemTask
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

        val packageFiles = packageProvider.resolveFiles({ resolver ->
            PropertyParser(project).filter(resolver.group, "aem.deploy.satisfy.group")
        })

        logger.info("Packages provided (${packageFiles.size})")

        // TODO await instance up after each group
        synchronizeInstances({ sync ->
            logger.info("Satisfying (uploading & installing)")

            packageFiles.onEach { packageFile ->
                installPackage(uploadPackage(packageFile, sync).path, sync)
            }
        })
    }

}