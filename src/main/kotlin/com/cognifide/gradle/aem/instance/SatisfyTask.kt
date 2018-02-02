package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.base.api.AemTask
import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.internal.file.resolver.FileGroup
import com.cognifide.gradle.aem.internal.file.resolver.FileResolver
import com.cognifide.gradle.aem.pkg.deploy.SyncTask
import groovy.lang.Closure
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.util.ConfigureUtil

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
        val packageGroups = providePackageGroups()
        satisfyPackagesOnInstances(packageGroups)
    }

    private fun providePackageGroups(): List<FileGroup> {
        logger.info("Providing packages from local and remote sources.")

        // TODO use here overridden file resolver which will wrap jars into CRX packages with support for restarting instance after satisfying
        val fileGroups = packageProvider.filterGroups(groupFilter)
        val files = fileGroups.flatMap { it.files }

        logger.info("Packages provided (${files.size}).")

        return fileGroups
    }

    private fun satisfyPackagesOnInstances(packageGroups: List<FileGroup>) {
        for (packageGroup in packageGroups) {
            logger.info("Satisfying group of packages '$group'.")

            var shouldAwait = false
            synchronizeInstances({ sync ->
                packageGroup.files.onEach {
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