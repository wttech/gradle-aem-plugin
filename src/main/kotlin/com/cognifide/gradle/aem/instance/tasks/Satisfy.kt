package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.names
import com.cognifide.gradle.aem.internal.Patterns
import com.cognifide.gradle.aem.pkg.Package
import com.cognifide.gradle.aem.pkg.resolver.PackageResolver
import com.cognifide.gradle.aem.pkg.tasks.Deploy
import java.io.File
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

open class Satisfy : Deploy() {

    /**
     * Determines which packages should be installed by default when satisfy task is being executed.
     */
    @Input
    var groupName = aem.props.string("aem.satisfy.group.name", "*")

    @get:Internal
    var groupFilter: (String) -> Boolean = { fileGroup -> Patterns.wildcard(fileGroup, groupName) }

    /**
     * Satisfy is a lazy task, which means that it will not install package that is already installed.
     * By default, information about currently installed packages is being retrieved from AEM only once.
     *
     * This flag can change that behavior, so that information will be refreshed after each package installation.
     */
    @Input
    var packageRefreshing: Boolean = aem.props.boolean("aem.satisfy.packageRefreshing", false)

    /**
     * Provides a packages from local and remote sources.
     * Handles automatic wrapping OSGi bundles to CRX packages.
     */
    @Nested
    val packageProvider = PackageResolver(project, AemTask.temporaryDir(project, name, DOWNLOAD_DIR))

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
        get() = project.findProperty("aem.satisfy.urls") != null

    init {
        group = AemTask.GROUP
        description = "Satisfies AEM by uploading & installing dependent packages on instance(s)."

        defineCmdGroups()
    }

    private fun defineCmdGroups() {
        if (cmdGroups) {
            aem.props.list("aem.satisfy.urls").forEachIndexed { index, url ->
                packageProvider.group("cmd.${index + 1}") { url(url) }
            }
        }
    }

    fun packages(configurer: PackageResolver.() -> Unit) {
        packageProvider.apply(configurer)
    }

    @TaskAction
    fun satisfy() {
        val actions = mutableListOf<PackageAction>()

        for (packageGroup in packageGroups) {
            logger.info("Satisfying group of packages '${packageGroup.name}'.")

            var packageSatisfiedAny = false
            val packageInstances = instances.filter { Patterns.wildcard(it.name, packageGroup.instanceName) }

            aem.sync(packageInstances) {
                val packageStates = packageGroup.files.fold(mutableMapOf<File, Package?>()) { states, pkg ->
                    states[pkg] = determineRemotePackage(pkg, packageRefreshing); states
                }
                val anyPackageSatisfiable = packageStates.any {
                    isSnapshot(it.key) || it.value == null || !it.value!!.installed
                }

                if (anyPackageSatisfiable) {
                    packageGroup.initializer(this)
                }

                packageStates.forEach { (pkg, state) ->
                    when {
                        isSnapshot(pkg) -> {
                            logger.lifecycle("Satisfying package ${pkg.name} on ${instance.name} (snapshot).")
                            deployPackage(pkg, uploadForce, uploadRetry, installRecursive, installRetry)

                            packageSatisfiedAny = true
                            actions.add(PackageAction(pkg, instance))
                        }
                        state == null -> {
                            logger.lifecycle("Satisfying package ${pkg.name} on ${instance.name} (not uploaded).")
                            deployPackage(pkg, uploadForce, uploadRetry, installRecursive, installRetry)

                            packageSatisfiedAny = true
                            actions.add(PackageAction(pkg, instance))
                        }
                        !state.installed -> {
                            logger.lifecycle("Satisfying package ${pkg.name} on ${instance.name} (not installed).")
                            installPackage(state.path, installRecursive, installRetry)

                            packageSatisfiedAny = true
                            actions.add(PackageAction(pkg, instance))
                        }
                        else -> {
                            logger.lifecycle("Not satisfying package: ${pkg.name} on ${instance.name} (already installed).")
                        }
                    }
                }

                if (anyPackageSatisfiable) {
                    packageGroup.finalizer(this)
                }
            }

            if (packageSatisfiedAny) {
                packageGroup.completer()
            }
        }

        if (actions.isNotEmpty()) {
            val packages = actions.map { it.pkg }.toSet()
            val instances = actions.map { it.instance }.toSet()

            if (packages.size == 1) {
                aem.notifier.notify("Package satisfied", "${packages.first().name} on ${instances.names}")
            } else {
                aem.notifier.notify("Packages satisfied", "Performed ${actions.size} action(s) for ${packages.size} package(s) on ${instances.size} instance(s).")
            }
        }
    }

    class PackageAction(val pkg: File, val instance: Instance)

    companion object {
        const val NAME = "aemSatisfy"

        const val DOWNLOAD_DIR = "download"
    }
}