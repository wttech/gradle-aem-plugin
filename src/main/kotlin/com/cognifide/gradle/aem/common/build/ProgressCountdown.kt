package com.cognifide.gradle.aem.common.build

import com.cognifide.gradle.aem.common.utils.Formats
import org.gradle.api.Project

class ProgressCountdown(project: Project, private val value: Long) {

    val logger = ProgressLogger.of(project)

    var loggerInterval = 100

    var progress: (Long) -> String = { "time left: ${Formats.duration(it)}" }

    fun run() {
        if (value <= 0) {
            return
        }

        val start = System.currentTimeMillis()

        logger.launch {
            while (true) {
                val current = System.currentTimeMillis()
                val delta = current - start
                val countdown = value - delta

                if (countdown <= 0) {
                    break
                }

                logger.progress(progress(countdown))
                Behaviors.waitFor(loggerInterval)
            }
        }
    }
}