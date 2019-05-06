package com.cognifide.gradle.aem.common

import java.util.*
import kotlinx.coroutines.*
import org.gradle.api.Project

class ProgressIndicator(private val project: Project) {

    var total = 0L

    var step = ""

    var message = ""

    var count = 0L

    var updater: ProgressIndicator.() -> Unit = { update() }

    private var delay = 100

    private val messageQueue: Queue<String> = LinkedList()

    private lateinit var logger: ProgressLogger

    private lateinit var timer: Behaviors.Timer

    fun <T> launch(block: ProgressIndicator.() -> T): T {
        return if (ProgressLogger.parents(project).isEmpty()) {
            ProgressLogger.of(project).launch {
                launchAsync(block) // progress logger requires some parent launched at main thread
            }
        } else {
            launchAsync(block)
        }
    }

    private fun <T> ProgressIndicator.launchAsync(block: ProgressIndicator.() -> T): T {
        return runBlocking {
            val blockJob = async(Dispatchers.Default) { block() }
            val loggerJob = async(Dispatchers.Default) {
                ProgressLogger.of(project).launch {
                    this@ProgressIndicator.logger = this
                    Behaviors.waitUntil(delay) { timer ->
                        this@ProgressIndicator.timer = timer
                        updater()
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

    fun reset() {
        message = ""
        count = 0
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

    fun update(message: String) {
        this.message = message
        update()
    }

    @Suppress("MagicNumber")
    private val text: String
        get() {
            val indicator = if (::timer.isInitialized && timer.ticks.rem(10L) < 5) {
                "\\"
            } else {
                "/"
            }

            val parts = mutableListOf<String>()
            if (total > 0) {
                parts += "$count/$total|${Formats.percent(count, total)}"
            }
            if (step.isNotEmpty()) {
                parts += step
            }
            val messageQueued = messageQueue.peek() ?: message
            if (messageQueued.isNotBlank()) {
                parts += messageQueued
            }

            return "$indicator ${parts.joinToString(" # ")}".trim()
        }
}