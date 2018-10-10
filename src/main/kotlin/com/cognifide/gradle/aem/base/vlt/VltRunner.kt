package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.pkg.PackagePlugin
import org.apache.commons.lang3.time.StopWatch
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File

// TODO https://github.com/Cognifide/gradle-aem-plugin/issues/135
class VltRunner(val project: Project) {

    val logger: Logger = project.logger

    val props = PropertyParser(project)

    val config = AemConfig.of(project)

    val cleaner = VltCleaner(project)

    val checkoutFilter by lazy { VltFilter.of(project) }

    val checkoutInstance: Instance by lazy { Instance.single(project) }

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

            return checkoutFilter.rootDirs(contentDir)
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

    fun rcp(): VltRcpSummary {
        var copiedPaths = 0L
        val stopWatch = StopWatch()

        stopWatch.start()
        rcpPaths.forEach { (sourcePath, targetPath) ->
            raw("rcp $rcpOpts ${rcpSourceInstance.httpBasicAuthUrl}/crx/-/jcr:root$sourcePath ${rcpTargetInstance.httpBasicAuthUrl}/crx/-/jcr:root$targetPath")
            copiedPaths++
        }
        stopWatch.stop()

        return VltRcpSummary(rcpSourceInstance, rcpTargetInstance, copiedPaths, stopWatch.time)
    }

    val rcpPaths: Sequence<Pair<String, String>>
        get() {
            val paths = props.list("aem.rcp.paths")
            if (paths.isNotEmpty()) {
                return paths.asSequence().map { path ->
                    rcpPathMapping(path)
                }
            }

            val pathsFilePath = props.string("aem.rcp.pathsFile")
            if (!pathsFilePath.isNullOrBlank()) {
                val pathsFile = File(pathsFilePath)
                if (!pathsFile.exists()) {
                    throw VltException("RCP paths file does not exist: $pathsFile")
                }

                return pathsFile.useLines { line -> line.map { rcpPathMapping(it) } }
            }

            throw VltException("RCP param '-Paem.rcp.paths' or '-Paem.rcp.pathsFile' must be specified.")
        }

    private fun rcpPathMapping(path: String): Pair<String, String> {
        val parts = path.trim().split("=").map { it.trim() }

        return when (parts.size) {
            1 -> Pair(path, path)
            2 -> Pair(parts[0], parts[1])
            else -> throw VltException("RCP path has invalid format: $path")
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