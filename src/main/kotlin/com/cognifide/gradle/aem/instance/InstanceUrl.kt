package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.base.api.AemException
import java.net.MalformedURLException
import java.net.URL

class InstanceUrl(raw: String) {

    val config = URL(raw)

    val user: String
        get() = userPart(0) ?: Instance.USER_DEFAULT

    val password: String
        get() = userPart(1) ?: Instance.PASSWORD_DEFAULT

    val httpUrl: String
        get() = "${config.protocol}://${config.host}:${config.port}"

    val type: String
        get() = InstanceType.nameByUrl(httpUrl)

    private fun userPart(index: Int): String? {
        return config.userInfo?.split(":")
                ?.takeIf { it.size == 2 }
                ?.get(index)
    }

    companion object {

        fun parse(raw: String): InstanceUrl {
            return try {
                InstanceUrl(raw)
            } catch (e: MalformedURLException) {
                throw AemException("Cannot parse instance URL: '$raw'", e)
            }
        }

    }

}