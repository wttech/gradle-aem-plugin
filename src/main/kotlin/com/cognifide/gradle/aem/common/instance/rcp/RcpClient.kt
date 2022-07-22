package com.cognifide.gradle.aem.common.instance.rcp

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.pkg.vault.VaultException
import org.apache.commons.lang3.time.StopWatch
import java.io.File

class RcpClient(private val aem: AemExtension) {

    var sourceInstance: Instance? = null

    var targetInstance: Instance? = null

    var paths: List<String>? = null

    var pathsFile: File? = null

    var workspace = "crx"

    var opts: String = "-b 100 -r -u"

    val stopWatch = StopWatch()

    var copiedPaths = 0L

    fun copy() {
        if (paths == null && pathsFile == null) {
            throw AemException("RCP need to have defined 'rcp.paths' or 'rcp.pathsFile'!")
        }

        paths?.let { copy(it) }
        pathsFile?.let { copy(it) }
    }

    fun copy(pathsFile: File) {
        if (!pathsFile.exists()) {
            throw VaultException("RCP paths file does not exist: $pathsFile")
        }

        pathsFile.useLines { copy(it) }
    }

    fun copy(paths: Sequence<String>) = paths.forEach { copy(it) }

    fun copy(paths: Iterable<String>) = paths.forEach { copy(it) }

    fun copy(paths: Map<String, String>) = paths.forEach { (sourcePath, targetPath) -> copy(sourcePath, targetPath) }

    fun copy(path: String) = pathMapping(path).let { copy(it.first, it.second) }

    fun copy(sourcePath: String, targetPath: String) {
        checkInstances()
        stopWatch.apply { if (!isStarted) start() else resume() }
        aem.vlt(
            "rcp $opts ${sourceInstance!!.httpUrlBasicAuth}/$workspace/-/jcr:root$sourcePath " +
                "${targetInstance!!.httpUrlBasicAuth}/$workspace/-/jcr:root$targetPath"
        )
        copiedPaths++
        stopWatch.suspend()
    }

    fun summary(): RcpSummary {
        checkInstances()
        return RcpSummary(sourceInstance!!, targetInstance!!, copiedPaths, stopWatch.time)
    }

    private fun pathMapping(path: String): Pair<String, String> {
        val parts = path.trim().split("=").map { it.trim() }

        return when (parts.size) {
            1 -> Pair(path, path)
            2 -> Pair(parts[0], parts[1])
            else -> throw VaultException("RCP path has invalid format: $path")
        }
    }

    private fun checkInstances() {
        if (sourceInstance == null) {
            throw VaultException("Source RCP instance is not defined.'")
        }

        if (targetInstance == null) {
            throw VaultException("Target RCP instance is not defined.")
        }
    }
}
