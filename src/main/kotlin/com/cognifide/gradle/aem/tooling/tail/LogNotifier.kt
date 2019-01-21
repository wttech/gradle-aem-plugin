package com.cognifide.gradle.aem.tooling.tail

import com.cognifide.gradle.aem.common.NotifierFacade
import com.cognifide.gradle.aem.tooling.tail.io.LogFiles
import java.net.URI
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch

@UseExperimental(ObsoleteCoroutinesApi::class)
class LogNotifier(
    private val notificationChannel: ReceiveChannel<ProblematicLogs>,
    private val notifier: NotifierFacade,
    private val logFiles: LogFiles
) {

    init {
        GlobalScope.launch {
            notificationChannel.consumeEach { logs ->
                val file = snapshotErrorsToSeparateFile(logs)
                notifier.notifyLogError("${logs.size} errors on ${logs.instanceName}", "Click to open incident log:\n${logs.logs.last().message}", file)
            }
        }
    }

    private fun snapshotErrorsToSeparateFile(logs: ProblematicLogs): URI {
        return logFiles.writeToSnapshot(logs.instanceName) { out ->
            logs.logs.forEach {
                out.write("${it.text}\n")
            }
        }
    }
}