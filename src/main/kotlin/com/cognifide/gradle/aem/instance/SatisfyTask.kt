package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.base.api.AemTask
import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.internal.file.FileResolver
import com.cognifide.gradle.aem.pkg.deploy.SyncTask
import groovy.lang.Closure
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.util.ConfigureUtil
import java.io.File

// TODO nested groups
// TODO group instanceName via method()
open class SatisfyTask : SyncTask() {

    companion object {
        val NAME = "aemSatisfy"

        val DOWNLOAD_DIR = "download"
    }

    @get:Internal
    val filesProvider = FileResolver(project, AemTask.temporaryDir(project, NAME, DOWNLOAD_DIR))


    @get:Internal
    var groupFilter: (String) -> Boolean = { fileGroup ->
        PropertyParser(project).filter(fileGroup, "aem.satisfy.group")
    }

    init {
        group = AemTask.GROUP
        description = "Satisfies AEM by uploading & installing dependent packages or bundles on instance(s)."
    }

    fun files(closure: Closure<*>) {
        ConfigureUtil.configure(closure, filesProvider)
    }

    @TaskAction
    fun satisfy() {
        val files = provideGroupedFiles()
        satisfyFilesOnInstances(files)
    }

    private fun provideGroupedFiles(): Map<String, List<File>> {
        logger.info("Providing files from local and remote sources.")

        val groupedFiles = filesProvider.groupedFiles(groupFilter)

        logger.info("Files provided (${groupedFiles.map { it.value.size }.sum()}).")

        return groupedFiles
    }

    private fun satisfyFilesOnInstances(groupedPackages: Map<String, List<File>>) {
        for ((group, files) in groupedPackages) {
            logger.info("Satisfying group of files '$group'.")

            var shouldAwait = false
            synchronizeInstances({ sync ->
                files.onEach {
                    if (sync.satisfyFile(it)) {
                        shouldAwait = true
                    }
                }
            })

            if (shouldAwait) {
                awaitStableInstances()
            }
        }
    }

}