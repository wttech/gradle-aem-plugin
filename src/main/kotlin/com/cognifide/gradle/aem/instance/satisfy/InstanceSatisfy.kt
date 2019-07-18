package com.cognifide.gradle.aem.instance.satisfy

import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.common.build.ProgressIndicator
import com.cognifide.gradle.aem.common.file.resolver.FileGroup
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.instance.service.pkg.PackageState
import com.cognifide.gradle.aem.common.utils.Patterns
import com.cognifide.gradle.aem.pkg.tasks.PackageDeploy
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

open class InstanceSatisfy : PackageDeploy() {

    /**
     * Forces to upload and install again all packages regardless their state on instances (already uploaded / installed).
     */
    @Input
    var greedy = aem.props.flag("instance.satisfy.greedy")

    /**
     * Determines which packages should be installed by default when satisfy task is being executed.
     */
    @Input
    var groupName = aem.props.string("instance.satisfy.group") ?: "*"

    @get:Internal
    var groupFilter: FileGroup.() -> Boolean = { Patterns.wildcard(name, groupName) }

    /**
     * Packages are installed lazy which means already installed will no be installed again.
     * By default, information about currently installed packages is being retrieved from AEM only once.
     *
     * This flag can change that behavior, so that information will be refreshed after each package installation.
     */
    @Input
    var listRefresh: Boolean = aem.props.boolean("instance.satisfy.listRefresh") ?: false

    /**
     * Repeat listing package when failed (brute-forcing).
     */
    @Internal
    @get:JsonIgnore
    var listRetry = aem.retry { afterSquaredSecond(aem.props.long("instance.satisfy.listRetry") ?: 3) }

    /**
     * Path in which downloaded CRX packages will be stored.
     */
    @Internal
    var downloadDir = aem.props.string("instance.satisfy.downloadDir")?.let { aem.project.file(it) }
            ?: AemTask.temporaryDir(project, name, "download")

    /**
     * Provides a packages from local and remote sources.
     * Handles automatic wrapping OSGi bundles to CRX packages.
     */
    @Nested
    val packageProvider = PackageResolver(aem, downloadDir)

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
            packageProvider.resolveGroups { Patterns.wildcard(name, groupName) }
        } else {
            logger.info("Providing packages defined in build script.")
            packageProvider.resolveGroups(groupFilter)
        }

        val files = result.flatMap { it.files }

        logger.info("Packages provided (${files.size}).")

        result
    }

    @get:Internal
    val cmdGroups: Boolean
        get() = project.findProperty("instance.satisfy.urls") != null

    private val packageActions = mutableListOf<PackageAction>()

    init {
        group = AemTask.GROUP
        description = "Satisfies AEM by uploading & installing dependent packages on instance(s)."

        defineCmdGroups()
    }

    private fun defineCmdGroups() {
        if (cmdGroups) {
            val urls = aem.props.list("instance.satisfy.urls") ?: listOf()
            urls.forEachIndexed { index, url ->
                packageProvider.group("cmd.${index + 1}") { download(url) }
            }
        }
    }

    fun packages(configurer: PackageResolver.() -> Unit) {
        packageProvider.apply(configurer)
    }

    @TaskAction
    @Suppress("ComplexMethod")
    override fun deploy() {
        checkInstances()

        aem.progress(packageGroups.sumBy { it.files.size * determineInstancesForGroup(it).size }) {
            packageGroups.forEach { satisfyGroup(it) }
        }

        if (packageActions.isNotEmpty()) {
            val packages = packageActions.map { it.pkg }.toSet()
            val instances = packageActions.map { it.instance }.toSet()

            if (packages.size == 1) {
                aem.notifier.notify("Package satisfied", "${packages.first().name} on ${instances.names}")
            } else {
                aem.notifier.notify("Packages satisfied", "Performed ${packageActions.size} action(s) for " +
                        "${packages.size} package(s) on ${instances.size} instance(s).")
            }
        } else {
            aem.logger.info("No actions to perform / all packages satisfied.")
        }
    }

    @Suppress("ComplexMethod")
    private fun ProgressIndicator.satisfyGroup(group: PackageGroup) {
        step = "Group '${group.name}'"

        aem.logger.info("Satisfying group of packages '${group.name}'.")

        var packageSatisfiedAny = false
        val packageInstances = determineInstancesForGroup(group)

        aem.sync(packageInstances) {
            val packageStates = group.files.map {
                PackageState(it, packageManager.find(it, listRefresh, listRetry))
            }
            val packageSatisfiableAny = packageStates.any {
                greedy || group.greedy || packageManager.isSnapshot(it.file) || !it.uploaded || !it.installed
            }

            if (packageSatisfiableAny) {
                apply(group.initializer)
            }

            packageStates.forEach { pkg ->
                increment("Satisfying package '${pkg.file.name}' on instance '${instance.name}'") {
                    when {
                        greedy || group.greedy -> {
                            aem.logger.info("Satisfying package ${pkg.name} on ${instance.name} (greedy).")

                            packageManager.deploy(pkg.file, uploadForce, uploadRetry, installRecursive, installRetry)

                            packageSatisfiedAny = true
                            packageActions.add(PackageAction(pkg.file, instance))
                        }
                        packageManager.isSnapshot(pkg.file) -> {
                            aem.logger.info("Satisfying package ${pkg.name} on ${instance.name} (snapshot).")
                            packageManager.deploy(pkg.file, uploadForce, uploadRetry, installRecursive, installRetry)

                            packageSatisfiedAny = true
                            packageActions.add(PackageAction(pkg.file, instance))
                        }
                        !pkg.uploaded -> {
                            aem.logger.info("Satisfying package ${pkg.name} on ${instance.name} (not uploaded).")
                            packageManager.deploy(pkg.file, uploadForce, uploadRetry, installRecursive, installRetry)

                            packageSatisfiedAny = true
                            packageActions.add(PackageAction(pkg.file, instance))
                        }
                        !pkg.installed -> {
                            aem.logger.info("Satisfying package ${pkg.name} on ${instance.name} (not installed).")
                            packageManager.install(pkg.state!!.path, installRecursive, installRetry)

                            packageSatisfiedAny = true
                            packageActions.add(PackageAction(pkg.file, instance))
                        }
                        else -> {
                            aem.logger.info("Not satisfying package: ${pkg.name} on ${instance.name} (already installed).")
                        }
                    }
                }
            }

            if (packageSatisfiableAny) {
                apply(group.finalizer)
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
        const val NAME = "instanceSatisfy"
    }
}