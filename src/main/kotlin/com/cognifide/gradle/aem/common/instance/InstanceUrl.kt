package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemException
import java.net.MalformedURLException
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder

class InstanceUrl(raw: String) {

    val config = URL(raw)

    val user: String? get() = userPart(0)

    val password: String? get() = userPart(1)

    val httpUrl: String get() = when {
        config.port != -1 -> "${config.protocol}://${config.host}:${config.port}"
        else -> "${config.protocol}://${config.host}"
    }

    val basicAuth: String? get() = when {
        user != null && password != null -> basicAuth(user!!, password!!)
        else -> null
    }

    fun basicAuth(user: String, password: String): String {
        val userInfo = "${encode(user)}:${encode(password)}"
        return when {
            config.port != -1 -> "${config.protocol}://$userInfo@${config.host}:${config.port}"
            else -> "${config.protocol}://$userInfo@${config.host}"
        }
    }

    val httpHost: String get() = config.host

    val httpPort: Int get() = when {
        config.port != -1 -> config.port
        else -> {
            when (config.protocol) {
                "https" -> HTTPS_PORT
                else -> HTTP_PORT
            }
        }
    }

    val name: String get() = "$env-$purposeId"

    val purposeId: String get() = purpose.name.lowercase()

    val purpose: Purpose get() = Purpose.byUrl(httpUrl)

    val debugPort: Int
        get() = if (config.port != -1) {
            "1$httpPort".toInt()
        } else {
            if (config.protocol == "https") {
                HTTPS_DEBUG_PORT
            } else {
                HTTP_DEBUG_PORT
            }
        }

    val env: String get() = when {
        ENV_LOCAL_HOSTS.contains(config.host) -> "local"
        else -> Purpose.trim(config.host)
            .replace(".", "_")
            .replace("-", "_")
    }

    private fun userPart(index: Int): String? {
        return config.userInfo?.split(":")
            ?.takeIf { it.size == 2 }
            ?.get(index)
            ?.let { decode(it) }
    }

    companion object {

        const val HTTP_HOST_DEFAULT = "http://localhost"

        const val HTTP_AUTHOR_PORT_DEFAULT = 4502

        const val HTTP_PUBLISH_PORT_DEFAULT = 4503

        const val HTTP_AUTHOR_DEFAULT = "$HTTP_HOST_DEFAULT:$HTTP_AUTHOR_PORT_DEFAULT"

        const val HTTP_PUBLISH_DEFAULT = "$HTTP_HOST_DEFAULT:$HTTP_PUBLISH_PORT_DEFAULT"

        const val HTTPS_PORT = 443

        const val HTTPS_DEBUG_PORT = 50443

        const val HTTP_PORT = 80

        const val HTTP_DEBUG_PORT = 50080

        val ENV_LOCAL_HOSTS = listOf("127.0.0.1", "localhost")

        fun encode(text: String): String = URLEncoder.encode(text, Charsets.UTF_8.name()) ?: text

        fun decode(text: String): String = URLDecoder.decode(text, Charsets.UTF_8.name()) ?: text

        fun parse(raw: String): InstanceUrl = try {
            InstanceUrl(raw)
        } catch (e: MalformedURLException) {
            throw AemException("Cannot parse instance URL: '$raw'", e)
        }
    }
}
