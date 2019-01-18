package com.cognifide.gradle.aem.common.notifier

import dorkbox.notify.Notify
import org.gradle.api.logging.LogLevel

interface Notifier {

    fun notify(title: String, text: String, level: LogLevel, clickActionHandler: (Notify) -> Unit = {})
}