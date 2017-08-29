package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.deploy.SyncTask
import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.internal.file.FileResolver
import groovy.lang.Closure
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.util.ConfigureUtil
import java.io.File

open class SatisfyTask : SyncTask() {

    companion object {
        val NAME = "aemSatisfy"

        val DOWNLOAD_DIR = "download"
    }

    @get:Internal
    val packageProvider = FileResolver(project, AemTask.temporaryDir(project, NAME, DOWNLOAD_DIR))


    @get:Internal
    var groupFilter: (String) -> Boolean = { fileGroup ->
        PropertyParser(project).filter(fileGroup, "aem.satisfy.group")
    }

    init {
        group = AemTask.GROUP
        description = "Satisfies AEM by uploading & installing dependent packages on instance(s)."
    }

    fun packages(closure: Closure<*>) {
        ConfigureUtil.configure(closure, packageProvider)
    }

    @TaskAction
    fun satisfy() {
        val packages = provideGroupedPackages()
        satisfyPackagesOnInstances(packages)
    }

    private fun provideGroupedPackages(): Map<String, List<File>> {
        logger.info("Providing packages from local and remote sources.")

        val groupedPackages = packageProvider.groupedFiles(groupFilter)

        logger.info("Packages provided (${groupedPackages.map { it.value.size }.sum()}).")

        return groupedPackages
    }

    private fun satisfyPackagesOnInstances(groupedPackages: Map<String, List<File>>) {
        for ((group, files) in groupedPackages) {
            logger.info("Satisfying group of packages '$group'.")

            var shouldAwait = false
            synchronizeInstances({ sync ->
                files.onEach {
                    if (sync.satisfyPackage(it)) {
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