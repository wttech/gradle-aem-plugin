package com.cognifide.gradle.aem.common

import java.util.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.gradle.api.Project

class ProgressIndicator(private val project: Project) {

    var delay = TimeUnit.SECONDS.toMillis(1)

    var total = 0L

    var step = ""

    var message = ""

    var count = 0L

    private val messageQueue: Queue<String> = LinkedList()

    private lateinit var logger: ProgressLogger

    private lateinit var timer: Behaviors.Timer

    fun <T> launch(block: ProgressIndicator.() -> T): T {
        return runBlocking {
            var done = false
            val blockJob = async(Dispatchers.Default) {
                try {
                    block()
                } finally {
                    done = true // TODO logger sometimes not dies
                }
            }

            ProgressLogger.of(project).launch {
                this@ProgressIndicator.logger = this
                Behaviors.waitUntil(delay) { timer ->
                    this@ProgressIndicator.timer = timer
                    update()
                    !done
                }
            }

            blockJob.await()
        }
    }

    fun increment(message: String, block: () -> Unit) {
        update()
        messageQueue.add(message)
        block()
        count++
        messageQueue.remove(message)
        update()
    }

    private fun update() {
        logger.progress(text)
    }

    private val text: String
        get() {
            var result = if (timer.ticks.rem(2L) == 0L) {
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