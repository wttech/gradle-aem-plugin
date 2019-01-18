package com.cognifide.gradle.aem.tooling.tail

import com.cognifide.gradle.aem.common.NotifierFacade
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch

@UseExperimental(ObsoleteCoroutinesApi::class)
class LogNotifier(
    private val notificationChannel: ReceiveChannel<ProblematicLogs>,
    private val notifier: NotifierFacade
) {

    init {
        GlobalScope.launch {
            notificationChannel.consumeEach { logs ->
                notifier.notify("${logs.size} errors on ${logs.instanceName}", logs.logs.last().message)
            }
        }
    }
}