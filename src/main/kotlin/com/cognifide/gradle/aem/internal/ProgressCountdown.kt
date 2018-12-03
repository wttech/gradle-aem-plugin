package com.cognifide.gradle.aem.internal

import org.gradle.api.Project

class ProgressCountdown(project: Project, header: String, private val value: Long) {

    val logger = ProgressLogger(project, header)

    var loggerInterval = 100

    var progress: (Long) -> String = { "time left: ${Formats.duration(it)}" }

    fun run() {
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