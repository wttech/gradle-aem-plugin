package com.cognifide.gradle.aem.tooling.rcp

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.utils.Formats
import com.cognifide.gradle.aem.tooling.vlt.VltRunner
import org.apache.commons.lang3.time.StopWatch

class RcpClient(
    private val runner: VltRunner,
    val sourceInstance: Instance,
    val targetInstance: Instance
) {

    var opts: String = ""

    val stopWatch = StopWatch()

    var copiedPaths = 0L

    fun copy(sourcePath: String, targetPath: String) {
        stopWatch.apply { if (!isStarted) start() else resume() }
        runner.apply {
            command = "rcp $opts ${sourceInstance.httpBasicAuthUrl}/crx/-/jcr:root$sourcePath ${targetInstance.httpBasicAuthUrl}/crx/-/jcr:root$targetPath"
            run()
            copiedPaths++
        }
        stopWatch.stop()
    }

    val summary: Summary
        get() = Summary(sourceInstance, targetInstance, copiedPaths, stopWatch.time)

    data class Summary(val source: Instance, val target: Instance, val copiedPaths: Long, val duration: Long) {

        val durationString: String
            get() = Formats.duration(duration)

        override fun toString(): String {
            return "VltRcpSummary(copiedPaths=$copiedPaths, duration=$durationString, source=$source, target=$target)"
        }
    }
}
