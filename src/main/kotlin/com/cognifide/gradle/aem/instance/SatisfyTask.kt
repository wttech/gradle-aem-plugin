package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.instance.satisfy.PackageResolver
import com.cognifide.gradle.aem.internal.Patterns
import com.cognifide.gradle.aem.pkg.deploy.ListResponse
import groovy.lang.Closure
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.util.ConfigureUtil
import java.io.File

open class SatisfyTask : AemDefaultTask() {

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
        val result = if (cmdGroups) {
            logger.info("Providing packages defined via command line.")
            packageProvider.filterGroups("cmd.*")
        } else {
            logger.info("Providing packages defined in build script.")
            packageProvider.filterGroups(groupFilter)
        }

        val files = result.flatMap { it.files }

        logger.info("Packages provided (${files.size}).")

        result
    }

    @get:Internal
    val cmdGroups: Boolean
        get() = project.properties["aem.satisfy.urls"] != null

    init {
        group = AemTask.GROUP
        description = "Satisfies AEM by uploading & installing dependent packages on instance(s)."

        defineCmdGroups()
    }

    private fun defineCmdGroups() {
        if (cmdGroups) {
            props.list("aem.satisfy.urls").forEachIndexed { index, url ->
                packageProvider.group("cmd.${index + 1}") { url(url) }
            }
        }
    }

    fun packages(configurer: Closure<PackageResolver>) {
        ConfigureUtil.configure(configurer, packageProvider)
    }

    fun packages(configurer: PackageResolver.() -> Unit) {
        packageProvider.apply(configurer)
    }

    @TaskAction
    fun satisfy() {
        val actions = mutableListOf<PackageAction>()

        for (packageGroup in packageGroups) {
            logger.info("Satisfying group of packages '${packageGroup.name}'.")

            var anyPackageSatisfied = false

            packageGroup.instances.sync(project) { sync ->
                val packageStates = packageGroup.files.fold(mutableMapOf<File, ListResponse.Package?>()) { states, pkg ->
                    states[pkg] = sync.determineRemotePackage(pkg, config.satisfyRefreshing); states
                }
                val anyPackageSatisfiable = packageStates.any {
                    sync.isSnapshot(it.key) || it.value == null || !it.value!!.installed
                }

                if (anyPackageSatisfiable) {
                    packageGroup.initializer(sync)
                }

                packageStates.forEach { (pkg, state) ->
                    when {
                        sync.isSnapshot(pkg) -> {
                            logger.lifecycle("Satisfying package ${pkg.name} on ${sync.instance.name} (snapshot).")
                            sync.deployPackage(pkg)

                            anyPackageSatisfied = true
                            actions.add(PackageAction(pkg, sync.instance))
                        }
                        state == null -> {
                            logger.lifecycle("Satisfying package ${pkg.name} on ${sync.instance.name} (not uploaded).")
                            sync.deployPackage(pkg)

                            anyPackageSatisfied = true
                            actions.add(PackageAction(pkg, sync.instance))
                        }
                        !state.installed -> {
                            logger.lifecycle("Satisfying package ${pkg.name} on ${sync.instance.name} (not installed).")
                            sync.installPackage(state.path)

                            anyPackageSatisfied = true
                            actions.add(PackageAction(pkg, sync.instance))
                        }
                        else -> {
                            logger.lifecycle("Not satisfying package: ${pkg.name} on ${sync.instance.name} (already installed).")
                        }
                    }
                }

                if (anyPackageSatisfiable) {
                    packageGroup.finalizer(sync)
                }
            }

            if (anyPackageSatisfied) {
                packageGroup.completer()
            }
        }

        if (actions.isNotEmpty()) {
            val packages = actions.map { it.pkg }.toSet()
            val instances = actions.map { it.instance }.toSet()

            if (packages.size == 1) {
                notifier.default("Package satisfied", "${packages.first().name} on ${instances.names}")
            } else {
                notifier.default("Packages satisfied", "Performed ${actions.size} action(s) for ${packages.size} package(s) on ${instances.size} instance(s).")
            }
        }
    }

    class PackageAction(val pkg: File, val instance: Instance)

    companion object {
        const val NAME = "aemSatisfy"

        const val DOWNLOAD_DIR = "download"
    }

}