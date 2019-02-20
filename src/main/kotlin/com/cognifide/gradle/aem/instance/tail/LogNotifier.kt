package com.cognifide.gradle.aem.instance.tail

import com.cognifide.gradle.aem.common.NotifierFacade
import com.cognifide.gradle.aem.instance.tail.io.LogFiles
import java.net.URI
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch

/**
 * TODO: Since coroutines API is still in experimental mode we would need to adapt to it's final API when released.
 * Please see https://github.com/Kotlin/kotlinx.coroutines/issues/632#issuecomment-425408865
 */
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
                notifyLogErrors(logs, file)
            }
        }
    }

    private fun notifyLogErrors(logs: ProblematicLogs, file: URI) {
        val errors = logs.size
        val message = logs.logs.lastOrNull()
                ?.message
                ?.splitToSequence("\n")
                ?.firstOrNull()
                ?.run { trim() }
                ?.substringAfter(" ")
                ?.capitalize()
                ?: ""
        val instance = logs.instanceName

        notifier.notifyLogError("$errors error(s) on $instance", message, file)
    }

    private fun snapshotErrorsToSeparateFile(logs: ProblematicLogs): URI {
        return logFiles.writeToIncident(logs.instanceName) { out ->
            logs.logs.forEach {
                out.write("${it.text}\n")
            }
        }
    }
}