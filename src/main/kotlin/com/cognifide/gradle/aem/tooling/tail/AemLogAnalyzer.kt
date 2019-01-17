package com.cognifide.gradle.aem.tooling.tail

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch

class AemLogAnalyzer(private val instanceName: String, private val logsChannel: Channel<Log>, private val notificationChannel: Channel<ProblematicLog>) {
    init {
        GlobalScope.launch {
            logsChannel.consumeEach { log ->
                when {
                    log.isOlderThan(minutes = 1) -> { }
                    log.isError() -> notificationChannel.send(ProblematicLog(instanceName, log))
                }
            }
        }
    }
}

class ProblematicLog(val instanceName: String, val log: Log)