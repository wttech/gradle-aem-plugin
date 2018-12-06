package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.AemTask
import com.cognifide.gradle.aem.common.Patterns
import com.cognifide.gradle.aem.common.ProgressIndicator
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.names
import com.cognifide.gradle.aem.pkg.PackageState
import com.cognifide.gradle.aem.pkg.resolver.PackageGroup
import com.cognifide.gradle.aem.pkg.resolver.PackageResolver
import com.cognifide.gradle.aem.pkg.tasks.Deploy
import java.io.File
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

open class Satisfy : Deploy() {

    /**
     * Forces to upload and install again all packages regardless their state on instances (already uploaded / installed).
     */
    @Input
    var greedy = aem.props.flag("aem.satisfy.greedy")

    /**
     * Determines which packages should be installed by default when satisfy task is being executed.
     */
    @Input
    var groupName = aem.props.string("aem.satisfy.groupName") ?: "*"

    @get:Internal
    var groupFilter: (String) -> Boolean = { fileGroup -> Patterns.wildcard(fileGroup, groupName) }

    /**
     * Satisfy is a lazy task, which means that it will not install package that is already installed.
     * By default, information about currently installed packages is being retrieved from AEM only once.
     *
     * This flag can change that behavior, so that information will be refreshed after each package installation.
     */
    @Input
    var packageRefreshing: Boolean = aem.props.boolean("aem.satisfy.packageRefreshing") ?: false

    /**
     * Provides a packages from local and remote sources.
     * Handles automatic wrapping OSGi bundles to CRX packages.
     */
    @Nested
    val packageProvider = PackageResolver(aem, AemTask.temporaryDir(project, name, DOWNLOAD_DIR))

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

    private val packageActions = mutableListOf<PackageAction>()

    init {
        group = AemTask.GROUP
        description = "Satisfies AEM by uploading & installing dependent packages on instance(s)."

        defineCmdGroups()
    }

    private fun defineCmdGroups() {
        if (cmdGroups) {
            val urls = aem.props.list("aem.satisfy.urls") ?: listOf()
            urls.forEachIndexed { index, url ->
                packageProvider.group("cmd.${index + 1}") { url(url) }
            }
        }
    }

    fun packages(configurer: PackageResolver.() -> Unit) {
        packageProvider.apply(configurer)
    }

    @TaskAction
    @Suppress("ComplexMethod")
    override fun deploy() {
        aem.progress({
            header = "Satisfying packages(s)"
            total = packageGroups.sumBy { packageGroup ->
                packageGroup.files.size * determineInstancesForGroup(packageGroup).size
            }.toLong()
        }, {
            packageGroups.forEach { satisfyGroup(it) }
        })

        if (packageActions.isNotEmpty()) {
            val packages = packageActions.map { it.pkg }.toSet()
            val instances = packageActions.map { it.instance }.toSet()

            if (packages.size == 1) {
                aem.notifier.notify("Package satisfied", "${packages.first().name} on ${instances.names}")
            } else {
                aem.notifier.notify("Packages satisfied", "Performed ${packageActions.size} action(s) for " +
                        "${packages.size} package(s) on ${instances.size} instance(s).")
            }
        }
    }

    @Suppress("ComplexMethod")
    private fun ProgressIndicator.satisfyGroup(group: PackageGroup) {
        logger.info("Satisfying group of packages '${group.name}'.")

        var packageSatisfiedAny = false
        val packageInstances = determineInstancesForGroup(group)

        aem.sync(packageInstances) {
            val packageStates = group.files.map {
                PackageState(it, determineRemotePackage(it, packageRefreshing))
            }
            val packageSatisfiableAny = packageStates.any {
                greedy || isSnapshot(it.file) || !it.uploaded || !it.installed
            }

            if (packageSatisfiableAny) {
                group.initializer(this)
            }

            packageStates.forEach { pkg ->
                increment("${group.name} # ${pkg.file.name} -> ${instance.name}") {
                    when {
                        greedy -> {
                            logger.info("Satisfying package ${pkg.name} on ${instance.name} (greedy).")

                            deployPackage(pkg.file, uploadForce, uploadRetry, installRecursive, installRetry)

                            packageSatisfiedAny = true
                            packageActions.add(PackageAction(pkg.file, instance))
                        }
                        isSnapshot(pkg.file) -> {
                            logger.info("Satisfying package ${pkg.name} on ${instance.name} (snapshot).")
                            deployPackage(pkg.file, uploadForce, uploadRetry, installRecursive, installRetry)

                            packageSatisfiedAny = true
                            packageActions.add(PackageAction(pkg.file, instance))
                        }
                        !pkg.uploaded -> {
                            logger.info("Satisfying package ${pkg.name} on ${instance.name} (not uploaded).")
                            deployPackage(pkg.file, uploadForce, uploadRetry, installRecursive, installRetry)

                            packageSatisfiedAny = true
                            packageActions.add(PackageAction(pkg.file, instance))
                        }
                        !pkg.installed -> {
                            logger.info("Satisfying package ${pkg.name} on ${instance.name} (not installed).")
                            installPackage(pkg.state!!.path, installRecursive, installRetry)

                            packageSatisfiedAny = true
                            packageActions.add(PackageAction(pkg.file, instance))
                        }
                        else -> {
                            logger.info("Not satisfying package: ${pkg.name} on ${instance.name} (already installed).")
                        }
                    }
                }
            }

            if (packageSatisfiableAny) {
                group.finalizer(this)
            }
        }

        if (packageSatisfiedAny) {
            group.completer()
        }
    }

    private fun determineInstancesForGroup(group: PackageGroup): List<Instance> {
        return instances.filter { Patterns.wildcard(it.name, group.instanceName) }
    }

    class PackageAction(val pkg: File, val instance: Instance)

    companion object {
        const val NAME = "aemSatisfy"

        const val DOWNLOAD_DIR = "download"
    }
}