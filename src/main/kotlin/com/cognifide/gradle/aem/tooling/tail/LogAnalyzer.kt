package com.cognifide.gradle.aem.tooling.tail

import com.cognifide.gradle.aem.tooling.tail.io.LogFiles
import com.cognifide.gradle.aem.tooling.tasks.Tail
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@UseExperimental(ObsoleteCoroutinesApi::class)
class LogAnalyzer(
    private val instanceName: String,
    private val logsChannel: ReceiveChannel<Log>,
    private val notificationChannel: SendChannel<ProblematicLogs>,
    vararg blacklists: String
) {

    private val errorsChannel = Channel<Log>(Channel.UNLIMITED)
    private var aggregatedErrors = mutableListOf<Log>()
    private val blacklist = Blacklist(blacklists)

    init {
        GlobalScope.launch {
            logsChannel.consumeEach { log ->
                when {
                    log.isOlderThan(minutes = 1) -> {
                    }
                    log.isError() && !blacklist.isBlacklisted(log) -> errorsChannel.send(log)
                }
            }
        }
        GlobalScope.launch {
            while (true) {
                val log = errorsChannel.poll()
                if (log == null) {
                    checkIfErrorsCannonadeEnded()
                    delay(CHECKING_FOR_ERROR_CANNONADE_TO_END_INTERVAL_IN_MILLIS)
                } else {
                    aggregatedErrors.add(log)
                }
            }
        }
    }

    private suspend fun checkIfErrorsCannonadeEnded() {
        if (aggregatedErrors.isEmpty()) return
        if (aggregatedErrors.last().isOlderThan(seconds = Tail.DELAY_TO_SHOW_NOTIFICATION_AFTER_LAST_ERROR_IN_SEC)) {
            notificationChannel.send(ProblematicLogs(instanceName, aggregatedErrors))
            aggregatedErrors = mutableListOf()
        }
    }

    companion object {
        private const val CHECKING_FOR_ERROR_CANNONADE_TO_END_INTERVAL_IN_MILLIS = 100L
    }
}

class ProblematicLogs(val instanceName: String, val logs: List<Log>) {
    val size = logs.size
}

class Blacklist(private val blacklists: Array<out String>) {
    private val blacklist = loadBlacklist()

    private fun loadBlacklist(): Map<String, Log> {
        val parser = Parser()
        return blacklists.map { logFile ->
            LogFiles.readClasspathOrPath(logFile) { reader ->
                parser.parseLogs(reader)
            }
        }.flatten().map { it.messageChecksum to it }.toMap()
    }

    fun isBlacklisted(log: Log) = blacklist.containsKey(log.messageChecksum)
}