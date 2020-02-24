package com.cognifide.gradle.aem.common.instance.satisfy

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceManager
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.instance.service.pkg.PackageState
import com.cognifide.gradle.common.utils.Patterns
import com.cognifide.gradle.common.file.resolver.FileGroup
import com.cognifide.gradle.common.utils.using
import java.io.File

class Satisfier(private val manager: InstanceManager) {

    private val aem = manager.aem

    private val common = aem.common

    private val logger = aem.logger

    /**
     * Enables deployment via CRX package activation from author to publishers when e.g they are not accessible.
     */
    val distributed = aem.obj.boolean {
        convention(false)
        aem.prop.boolean("instance.satisfy.distributed")?.let { set(it) }
    }

    /**
     * Forces to upload and install again all packages regardless their state on instances (already uploaded / installed).
     */
    val greedy = aem.obj.boolean {
        convention(false)
        aem.prop.boolean("instance.satisfy.greedy")?.let { set(it) }
    }

    /**
     * Determines which packages should be installed by default when satisfy task is being executed.
     */
    val groupName = aem.obj.string {
        convention("*")
        aem.prop.string("instance.satisfy.group")?.let { set(it) }
    }

    private var groupFilter: FileGroup.() -> Boolean = { Patterns.wildcard(name, groupName.get()) }

    fun groupFilter(predicate: FileGroup.() -> Boolean) {
        this.groupFilter = predicate
    }

    /**
     * Provides a packages from local and remote sources.
     * Handles automatic wrapping OSGi bundles to CRX packages.
     */
    fun packages(configurer: PackageResolver.() -> Unit) = packageProvider.using(configurer)

    private val packageProvider = PackageResolver(aem).apply {
        downloadDir.convention(manager.buildDir.dir("satisfy/packages"))
        aem.prop.file("instance.satisfy.downloadDir")?.let { downloadDir.set(it) }
        aem.prop.list("instance.satisfy.urls")?.forEachIndexed { index, url ->
            val no = index + 1
            val fileName = url.substringAfterLast("/").substringBeforeLast(".")
            group("$GROUP_CMD.$no.$fileName") { get(url) }
        }
    }

    private val packageGroups by lazy {
        val result = if (cmdGroups) {
            logger.info("Providing packages defined via command line.")
            packageProvider.allGroups { Patterns.wildcard(name, "$GROUP_CMD.*") }
        } else {
            logger.info("Providing packages defined in build script.")
            packageProvider.allGroups(groupFilter)
        }

        val files = result.flatMap { it.files }

        logger.info("Packages provided (${files.size}):\n${files.joinToString("\n")}")

        result
    }

    private val cmdGroups: Boolean get() = aem.prop.list("instance.satisfy.urls") != null

    fun satisfy(instance: Instance) = satisfy(listOf(instance))

    @Suppress("ComplexMethod")
    fun satisfy(instances: Collection<Instance>): List<PackageAction> {
        val allActions = mutableListOf<PackageAction>()

        common.progress(packageGroups.sumBy { it.files.size * groupInstances(instances, it).size }) {
            packageGroups.forEach { group ->
                step = "Group '${group.name}'"

                logger.info("Satisfying group of packages '${group.name}'.")

                val groupInstances = groupInstances(instances, group)
                val groupActions = mutableListOf<PackageAction>()

                aem.sync(groupInstances) {
                    val packageStates = group.files.map {
                        PackageState(it, packageManager.find(it))
                    }
                    val packageSatisfiableAny = packageStates.any {
                        (group.greedy.orNull ?: greedy.get()) || packageManager.isSnapshot(it.file) || !it.uploaded || !it.installed
                    }

                    if (packageSatisfiableAny) {
                        apply(group.initializer)
                    }

                    packageStates.forEach { pkg ->
                        increment("Satisfying package '${pkg.file.name}' on instance '${instance.name}'") {
                            when {
                                group.greedy.orNull ?: greedy.get() -> {
                                    logger.info("Satisfying package ${pkg.name} on ${instance.name} (greedy).")
                                    groupActions.add(satisfyPackage(group, pkg))
                                }
                                packageManager.isSnapshot(pkg.file) -> {
                                    logger.info("Satisfying package ${pkg.name} on ${instance.name} (snapshot).")
                                    groupActions.add(satisfyPackage(group, pkg))
                                }
                                !pkg.uploaded -> {
                                    logger.info("Satisfying package ${pkg.name} on ${instance.name} (not uploaded).")
                                    groupActions.add(satisfyPackage(group, pkg))
                                }
                                !pkg.installed -> {
                                    logger.info("Satisfying package ${pkg.name} on ${instance.name} (not installed).")
                                    groupActions.add(satisfyPackage(group, pkg))
                                }
                                else -> {
                                    logger.info("Not satisfying package: ${pkg.name} on ${instance.name} (already installed).")
                                }
                            }
                        }
                    }

                    if (packageSatisfiableAny) {
                        apply(group.finalizer)
                    }
                }

                if (groupActions.isNotEmpty()) {
                    group.completer(groupInstances)
                }

                allActions.addAll(groupActions)
            }
        }

        if (allActions.isEmpty()) {
            logger.lifecycle("All packages satisfied (no actions to perform).")
        }

        return allActions
    }

    private fun groupInstances(instances: Collection<Instance>, group: PackageGroup) = instances.filter {
        Patterns.wildcard(it.name, group.instanceName.get())
    }

    private fun InstanceSync.satisfyPackage(group: PackageGroup, state: PackageState): PackageAction {
        packageManager.deploy(state.file, group.distributed.orNull ?: distributed.get())
        return PackageAction(state.file, instance)
    }

    private val allFiles: List<File> get() = packageProvider.allFiles(groupFilter)

    fun resolve() {
        logger.info("Resolving CRX packages for satisfying instances")
        logger.info("Resolved CRX packages:\n${allFiles.joinToString("\n")}")
    }

    companion object {
        const val NAME = "instanceSatisfy"

        const val GROUP_CMD = "cmd"
    }
}
