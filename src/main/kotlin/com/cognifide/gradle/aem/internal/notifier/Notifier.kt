package com.cognifide.gradle.aem.internal.notifier

import org.gradle.api.logging.LogLevel

interface Notifier {

    fun notify(title: String, text: String, level: LogLevel)

}