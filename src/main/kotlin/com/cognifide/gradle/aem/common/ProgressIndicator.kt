package com.cognifide.gradle.aem.common

import java.util.*
import kotlinx.coroutines.*
import org.gradle.api.Project

class ProgressIndicator(private val project: Project) {

    var delay = 100

    var total = 0L

    var step = ""

    var message = ""

    var count = 0L

    private val messageQueue: Queue<String> = LinkedList()

    private lateinit var logger: ProgressLogger

    private lateinit var timer: Behaviors.Timer

    fun <T> launch(block: ProgressIndicator.() -> T): T {
        return runBlocking {
            val blockJob = async(Dispatchers.Default) { block() }
            val loggerJob = async(Dispatchers.Default) {
                ProgressLogger.of(project).launch {
                    this@ProgressIndicator.logger = this
                    Behaviors.waitUntil(delay) { timer ->
                        this@ProgressIndicator.timer = timer
                        update()
                        isActive
                    }
                }
            }

            loggerJob.start()
            val result = blockJob.await()
            loggerJob.cancelAndJoin()
            result
        }
    }

    fun increment(message: String) {
        this.message = message
        count++
    }

    fun <T> increment(message: String, block: () -> T): T {
        update()
        messageQueue.add(message)
        val result = block()
        count++
        messageQueue.remove(message)
        update()
        return result
    }

    fun update() {
        if (::logger.isInitialized) {
            logger.progress(text)
        }
    }

    private val text: String
        get() {
            var result = if (::timer.isInitialized && timer.ticks.rem(2L) == 0L) {
                "\\"
            } else {
                "/"
            }

            if (total > 0) {
                result = "$result $count/$total|${Formats.percent(count, total)}"
            }

            if (step.isNotEmpty()) {
                result = "$result # $step"
            }

            val messageQueued = messageQueue.peek() ?: message
            if (messageQueued.isNotBlank()) {
                result = "$result | $messageQueued"
            }

            return result
        }
}