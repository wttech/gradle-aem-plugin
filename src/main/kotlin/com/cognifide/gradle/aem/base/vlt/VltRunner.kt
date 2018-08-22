package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.internal.file.FileOperations
import com.cognifide.gradle.aem.pkg.PackagePlugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File

// TODO https://github.com/Cognifide/gradle-aem-plugin/issues/135
class VltRunner(val project: Project) {

    val logger: Logger = project.logger

    val props = PropertyParser(project)

    val config = AemConfig.of(project)

    val cleaner = VltCleaner(project)

    val workingDir: File
        get() {
            var path = "${config.contentPath}/${PackagePlugin.JCR_ROOT}"

            val relativePath = project.properties["aem.vlt.path"] as String?
            if (!relativePath.isNullOrBlank()) {
                path = "$path/$relativePath"
            }

            return File(path)
        }

    val contentDir: File
        get() = File(config.contentPath)

    val checkoutFilterRootDirs: List<File>
        get() {
            if (!contentDir.exists()) {
                logger.warn("JCR content directory does not exist: ${contentDir.absolutePath}")
                return listOf()
            }

            val roots = checkoutFilter.rootDirs(contentDir)
            if (roots.isEmpty()) {
                logger.warn("For given filter there is no existing root directories.")
                return listOf()
            }

            return roots
        }

    fun raw(command: String, props: Map<String, Any> = mapOf()) {
        val app = VltApp(project)

        val allProps = mapOf("instances" to config.instances) + props
        val fullCommand = this.props.expand(command, allProps)

        logger.lifecycle("Working directory: $workingDir")
        logger.lifecycle("Executing command: vlt $command")

        app.execute(fullCommand, workingDir.absolutePath)
    }

    fun checkout() {
        val contentDir = File(config.contentPath)
        if (!contentDir.exists()) {
            logger.info("JCR content directory to be checked out does not exist: ${contentDir.absolutePath}")
        }

        raw("--credentials ${checkoutInstance.credentials} checkout --force --filter ${checkoutFilter.file.absolutePath} ${checkoutInstance.httpUrl}/crx/server/crx.default")
    }

    val checkoutFilter by lazy { determineCheckoutFilter() }

    private fun determineCheckoutFilter(): VltFilter {
        val cmdFilterRoots = props.list("aem.checkout.filterRoots")

        return if (cmdFilterRoots.isNotEmpty()) {
            logger.info("Using Vault filter roots specified as command line property: $cmdFilterRoots")
            VltFilter.temporary(project, cmdFilterRoots)
        } else {
            if (config.checkoutFilterPath.isNotBlank()) {
                val configFilter = FileOperations.find(project, config.vaultPath, config.checkoutFilterPath)
                        ?: throw VltException("Vault check out filter file does not exist at path: ${config.checkoutFilterPath} (or under directory: ${config.vaultPath}).")
                VltFilter(configFilter)
            } else {
                val conventionFilter = FileOperations.find(project, config.vaultPath, config.checkoutFilterPaths)
                        ?: throw VltException("None of Vault check out filter file does not exist at one of convention paths: ${config.checkoutFilterPaths}.")
                VltFilter(conventionFilter)
            }
        }
    }

    val checkoutInstance: Instance by lazy { determineCheckoutInstance() }

    private fun determineCheckoutInstance(): Instance {
        val cmdInstanceArg = props.string("aem.checkout.instance")
        if (!cmdInstanceArg.isNullOrBlank()) {
            val cmdInstance = config.parseInstance(cmdInstanceArg!!)

            logger.info("Using instance specified by command line parameter: $cmdInstance")
            return cmdInstance
        }

        val namedInstance = Instance.filter(project, config.instanceName).firstOrNull()
        if (namedInstance != null) {
            logger.info("Using first instance matching filter '${config.instanceName}': $namedInstance")
            return namedInstance
        }

        val anyInstance = Instance.filter(project, Instance.FILTER_ANY).firstOrNull()
        if (anyInstance != null) {
            logger.info("Using first instance matching filter '${Instance.FILTER_ANY}': $anyInstance")
            return anyInstance
        }

        throw VltException("Vault instance cannot be determined neither by command line parameter nor AEM config.")
    }

    fun cleanBeforeCheckout() {
        logger.info("Preparing files to be cleaned up (before checking out new ones) using filter: $checkoutFilter")

        checkoutFilterRootDirs.forEach { root ->
            logger.lifecycle("Preparing root: $root")
            cleaner.prepare(root)
        }
    }

    fun cleanAfterCheckout() {
        logger.info("Cleaning using $checkoutFilter")

        checkoutFilterRootDirs.forEach { root ->
            logger.lifecycle("Cleaning root: $root")
            cleaner.clean(root)
        }
    }

    fun rcp() {
        rcpPaths.forEach { sourcePath, targetPath ->
            raw("rcp $rcpOpts ${rcpSourceInstance.httpBasicAuthUrl}/crx/-/jcr:root$sourcePath ${rcpTargetInstance.httpBasicAuthUrl}/crx/-/jcr:root$targetPath")
        }
    }

    val rcpPaths: Map<String, String> by lazy {
        val paths = props.list("aem.rcp.paths")
        if (paths.isEmpty()) {
            throw VltException("RCP param '-Paem.rcp.paths' is not specified.")
        }

        paths.fold(mutableMapOf<String, String>()) { r, path ->
            val parts = path.split("=").map { it.trim() }
            when (parts.size) {
                1 -> r[path] = path
                2 -> r[parts[0]] = parts[1]
                else -> throw VltException("RCP path has invalid format: $path")
            }
            r
        }
    }

    val rcpOpts: String by lazy { project.properties["aem.rcp.opts"] as String? ?: "-b 100 -r -u" }

    val rcpSourceInstance: Instance by lazy {
        config.parseInstance(project.properties["aem.rcp.source.instance"] as String?
                ?: throw VltException("RCP param '-Paem.rcp.source.instance' is not specified."))
    }

    val rcpTargetInstance: Instance by lazy {
        config.parseInstance(project.properties["aem.rcp.target.instance"] as String?
                ?: throw VltException("RCP param '-Paem.rcp.target.instance' is not specified."))
    }

}