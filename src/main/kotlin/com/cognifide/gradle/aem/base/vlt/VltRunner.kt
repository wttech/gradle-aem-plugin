package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.api.AemExtension
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.pkg.PackagePlugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File

// TODO https://github.com/Cognifide/gradle-aem-plugin/issues/135
class VltRunner(val project: Project) {

    val logger: Logger = project.logger

    val props = PropertyParser(project)

    val aem = AemExtension.of(project)

    val cleaner = VltCleaner(project)

    val checkoutFilter by lazy { VltFilter.of(project) }

    val checkoutInstance: Instance by lazy { Instance.single(project) }

    val workingDir: File
        get() {
            var path = "${aem.compose.contentPath}/${PackagePlugin.JCR_ROOT}"

            val relativePath = project.properties["aem.vlt.path"] as String?
            if (!relativePath.isNullOrBlank()) {
                path = "$path/$relativePath"
            }

            return File(path)
        }

    val contentDir: File
        get() = File(aem.compose.contentPath)

    val checkoutFilterRootDirs: List<File>
        get() {
            if (!contentDir.exists()) {
                logger.warn("JCR content directory does not exist: ${contentDir.absolutePath}")
                return listOf()
            }

            return checkoutFilter.rootDirs(contentDir)
        }

    fun raw(command: String, props: Map<String, Any> = mapOf()) {
        val app = VltApp(project)

        val allProps = mapOf("instances" to aem.config.instances) + props
        val fullCommand = this.props.expand(command, allProps)

        logger.lifecycle("Working directory: $workingDir")
        logger.lifecycle("Executing command: vlt $command")

        app.execute(fullCommand, workingDir.absolutePath)
    }

    fun checkout() {
        val contentDir = File(aem.compose.contentPath)
        if (!contentDir.exists()) {
            logger.info("JCR content directory to be checked out does not exist: ${contentDir.absolutePath}")
        }

        raw("--credentials ${checkoutInstance.credentials} checkout --force --filter ${checkoutFilter.file.absolutePath} ${checkoutInstance.httpUrl}/crx/server/crx.default")
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

    val rcpOpts: String by lazy { aem.props.string("aem.rcp.opts") ?: "-b 100 -r -u" }

    val rcpSourceInstance: Instance by lazy {
        aem.instance(aem.props.string("aem.rcp.source.instance")
                ?: throw VltException("RCP param '-Paem.rcp.source.instance' is not specified."))
    }

    val rcpTargetInstance: Instance by lazy {
        aem.instance(aem.props.string("aem.rcp.target.instance")
                ?: throw VltException("RCP param '-Paem.rcp.target.instance' is not specified."))
    }

}