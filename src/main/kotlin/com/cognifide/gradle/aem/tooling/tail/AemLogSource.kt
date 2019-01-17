package com.cognifide.gradle.aem.tooling.tail

import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.tooling.tasks.Tail
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class AemLogSource(private val aemInstance: Instance) : LogSource {

    override fun nextReader(): BufferedReader {
        val connection = URL("${aemInstance.httpUrl}$ERROR_LOG_ENDPOINT").openConnection() as HttpURLConnection
        val auth = Base64.getEncoder().encode(("${aemInstance.user}:${aemInstance.password}").toByteArray()).toString(Charsets.UTF_8)
        connection.addRequestProperty("Authorization", "Basic $auth")
        connection.connect()
        return BufferedReader(InputStreamReader(connection.inputStream))
    }

    companion object {
        const val ERROR_LOG_ENDPOINT = "/system/console/slinglog/tailer.txt" +
                "?_dc=1520834477194" +
                "&tail=${Tail.NUMBER_OF_LINES_READ_EACH_TIME}" +
                "&name=%2Flogs%2Ferror.log"
    }
}