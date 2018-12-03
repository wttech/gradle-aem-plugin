package com.cognifide.gradle.aem.internal

import java.util.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import org.gradle.api.Project

class ProgressIndicator(private val project: Project) {

    private val messageQueue: Queue<String> = LinkedList()

    var delay = TimeUnit.SECONDS.toMillis(1)

    var header = ""

    var message = ""

    var total = 0L

    var count = 0L

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

            ProgressLogger(project, header).launch {
                Behaviors.waitUntil(delay) { timer ->
                    var text = if (timer.ticks.rem(2L) == 0L) {
                        "\\"
                    } else {
                        "/"
                    }

                    if (total > 0) {
                        text = "$text $count/$total|${Formats.percent(count, total)}"
                    }

                    val messageQueued = if (messageQueue.isEmpty()) message else messageQueue.peek()
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
}