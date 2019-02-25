package com.cognifide.gradle.aem.instance.tail

import com.cognifide.gradle.aem.instance.Instance
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
class InstanceAnalyzer(
    private val options: TailOptions,
    private val instance: Instance,
    private val logsChannel: ReceiveChannel<Log>,
    private val notificationChannel: SendChannel<LogChunk>
) {

    private val incidentChannel = Channel<Log>(Channel.UNLIMITED)

    private var incidentCannonade = mutableListOf<Log>()

    fun listenTailed() {
        GlobalScope.launch {
            logsChannel.consumeEach { log ->
                options.logListener(log, instance)

                if (options.incidentChecker(log, instance)) {
                    incidentChannel.send(log)
                }
            }
        }
        GlobalScope.launch {
            while (true) {
                val log = incidentChannel.poll()
                if (log != null) {
                    incidentCannonade.add(log)
                }

                if (incidentCannonade.lastOrNull()?.isOlderThan(instance, options.incidentDelay) == true) {
                    notificationChannel.send(LogChunk(instance, incidentCannonade))
                    incidentCannonade = mutableListOf()
                }

                delay(INCIDENT_CANNONADE_END_INTERVAL)
            }
        }
    }

    companion object {

        private const val INCIDENT_CANNONADE_END_INTERVAL = 100L
    }
}
