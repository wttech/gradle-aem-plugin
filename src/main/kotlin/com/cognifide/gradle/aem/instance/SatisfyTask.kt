package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.instance.satisfy.PackageGroup
import com.cognifide.gradle.aem.instance.satisfy.PackageResolver
import com.cognifide.gradle.aem.internal.Patterns
import com.cognifide.gradle.aem.pkg.deploy.ListResponse
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

        val packageGroups = packageProvider.filterGroups(groupFilter)
        val packageFiles = packageGroups.flatMap { it.files }

        logger.info("Packages provided (${packageFiles.size}).")

        @Suppress("unchecked_cast")
        packageGroups as List<PackageGroup>
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
            logger.info("Satisfying group of packages '${packageGroup.name}'.")

            var anyPackageSatisfied = false

            synchronizeInstances({ sync ->
                val packageStates = packageGroup.files.fold(mutableMapOf<File, ListResponse.Package?>(), { states, pkg ->
                    states[pkg] = sync.determineRemotePackage(pkg, config.satisfyRefreshing); states
                })
                val anyPackageSatisfiable = packageStates.any {
                    sync.isSnapshot(it.key) || it.value == null || !it.value!!.installed
                }

                if (anyPackageSatisfiable) {
                    packageGroup.initializer(sync)
                }

                packageStates.forEach { (pkg, state) ->
                    when {
                        sync.isSnapshot(pkg) -> {
                            logger.info("Satisfying package: $pkg (snapshot).")
                            sync.deployPackage(pkg)
                            anyPackageSatisfied = true
                        }
                        state == null -> {
                            logger.info("Satisfying package: $pkg (not uploaded).")
                            sync.deployPackage(pkg)
                            anyPackageSatisfied = true
                        }
                        !state.installed -> {
                            logger.info("Satisfying package: $pkg (not installed).")
                            sync.installPackage(state.path)
                            anyPackageSatisfied = true
                        }
                        else -> {
                            logger.info("Not satisfying package: $pkg (already installed).")
                        }
                    }
                }

                if (anyPackageSatisfiable) {
                    packageGroup.finalizer(sync)
                }
            }, packageGroup.instances)

            if (anyPackageSatisfied) {
                packageGroup.completer()
            }
        }
    }

}