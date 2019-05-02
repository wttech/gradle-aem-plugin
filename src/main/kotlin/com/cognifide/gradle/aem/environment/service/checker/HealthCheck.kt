package com.cognifide.gradle.aem.environment.service.checker

import java.net.HttpURLConnection
import java.net.URI

class HealthCheck(val url: String) {

    val uri = URI(url)

    var status = HttpURLConnection.HTTP_OK

    var text: String = ""

    var maxAwaitTime = 10000L

    var connectionTimeout = 3000
}