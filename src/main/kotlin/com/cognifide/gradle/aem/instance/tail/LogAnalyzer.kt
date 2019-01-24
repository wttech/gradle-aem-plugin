package com.cognifide.gradle.aem.instance.tail

import com.cognifide.gradle.aem.common.file.FileOperations
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * TODO: Since coroutines API is still in experimental mode we would need to adapt to it's final API when released.
 * Please see https://github.com/Kotlin/kotlinx.coroutines/issues/632#issuecomment-425408865
 */
@UseExperimental(ObsoleteCoroutinesApi::class)
class LogAnalyzer(
    private val options: TailOptions,
    private val instanceName: String,
    private val logsChannel: ReceiveChannel<Log>,
    private val notificationChannel: SendChannel<ProblematicLogs>,
    private val blacklist: Blacklist = Blacklist()
) {

    private val errorsChannel = Channel<Log>(Channel.UNLIMITED)
    private var aggregatedErrors = mutableListOf<Log>()

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
        if (aggregatedErrors.last().isOlderThan(millis = options.notificationDelay)) {
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

class Blacklist(
    private val filters: List<(Log) -> Boolean> = emptyList(),
    private val blacklists: List<String> = emptyList()
) {

    private val parser = Parser()
    private val blacklist = loadBlacklists()

    private fun loadBlacklists(): Map<String, Log> {
        return (loadDefaultBlacklists(TailOptions.BLACKLIST_FILES_DEFAULT) +
                loadConfiguredBlacklists(blacklists))
                .flatten().map { it.messageChecksum to it }.toMap()
    }

    private fun loadConfiguredBlacklists(blacklists: List<String>): List<List<Log>> {
        return blacklists.map { logFile ->
            FileOperations.fromPathOrClasspath(logFile) { reader ->
                parser.parseLogs(reader)
            }
        }
    }

    private fun loadDefaultBlacklists(blacklists: List<String>): List<List<Log>> {
        return blacklists.mapNotNull { logFile ->
            FileOperations.optionalFromPathOrClasspath(logFile) { reader ->
                parser.parseLogs(reader)
            }
        }
    }

    fun isBlacklisted(log: Log) = blacklist.containsKey(log.messageChecksum) || !filters.none { it(log) }
}