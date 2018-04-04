package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.instance.satisfy.PackageGroup
import com.cognifide.gradle.aem.instance.satisfy.PackageResolver
import com.cognifide.gradle.aem.internal.Patterns
import com.cognifide.gradle.aem.pkg.deploy.SyncTask
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
    val packageProvider = PackageResolver(project, AemTask.temporaryDir(project, NAME, DOWNLOAD_DIR))

    @get:Internal
    var groupFilter: (String) -> Boolean = { fileGroup ->
        Patterns.wildcards(fileGroup, config.satisfyGroupName)
    }

    @get:Internal
    val outputDirs: List<File>
        get() = packageProvider.outputDirs(groupFilter)

    @get:Internal
    val allFiles: List<File>
        get() = packageProvider.allFiles(groupFilter)

    @get:Internal
    val packageGroups by lazy {
        logger.info("Providing packages from local and remote sources.")

        val fileGroups = packageProvider.filterGroups(groupFilter)
        val files = fileGroups.flatMap { it.files }

        logger.info("Packages provided (${files.size}).")

        // TODO It is possible to avoid this by introducing parametrized type e.g FileResolver<PackageGroup>
        @Suppress("unchecked_cast")
        fileGroups as List<PackageGroup>
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
        for (packageGroup in packageGroups) {
            logger.info("Satisfying group of packages '$group'.")

            val instances = if (config.deployDistributed) {
                Instance.filter(project, config.deployInstanceAuthorName)
            } else {
                filterInstances()
            }.filter { Patterns.wildcard(it.name, packageGroup.instance) }

            var satisfied = false
            synchronizeInstances({ sync ->
                packageGroup.files.onEach {
                    if (sync.satisfyPackage(it)) {
                        satisfied = true
                    }
                }
            }, instances)

            if (satisfied) {
                packageGroup.afterSatisfy(instances)
            }
        }
    }

}