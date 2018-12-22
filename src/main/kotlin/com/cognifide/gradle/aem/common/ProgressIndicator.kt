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

    var message = ""

    var count = 0L

    var hold = false

    private val messageQueue: Queue<String> = LinkedList()

    fun <T> launch(block: ProgressIndicator.() -> T): T {
        return runBlocking {
            var done = false
            val blockJob = async(Dispatchers.Default) {
                try {
                    block()
                } finally {
                    done = true
                }
            }

            ProgressLogger.of(project).launch {
                Behaviors.waitUntil(delay) { timer ->
                    if (hold) {
                        return@waitUntil true
                    }

                    var text = if (timer.ticks.rem(2L) == 0L) {
                        "\\"
                    } else {
                        "/"
                    }

                    if (total > 0) {
                        text = "$text $count/$total|${Formats.percent(count, total)}"
                    }

                    val messageQueued = messageQueue.peek() ?: message
                    if (messageQueued.isNotBlank()) {
                        text = "$text | $messageQueued"
                    }

                    progress(text)

                    !done
                }
            }

            blockJob.await()
        }
    }

    fun increment(message: String, block: () -> Unit) {
        messageQueue.add(message)
        block()
        count++
        messageQueue.remove(message)
    }

    fun hold(block: () -> Unit) {
        hold = true
        block()
        hold = false
    }
}