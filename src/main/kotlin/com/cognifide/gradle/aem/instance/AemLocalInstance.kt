package com.cognifide.gradle.aem.instance

import java.io.Serializable

class AemLocalInstance(
        override val httpUrl: String,
        override val user: String,
        override val password: String,
        override val typeName: String,
        val debugPort: Int
) : AemInstance, Serializable {

    companion object {
        val ENVIRONMENT = "local"

        fun debugPortByUrl(url: String): Int {
            return "1${AemInstance.portOfUrl(url)}".toInt()
        }
    }

    constructor(httpUrl: String, user: String, password: String) : this(
            httpUrl,
            user,
            password,
            AemInstanceType.byUrl(httpUrl).name,
            debugPortByUrl(httpUrl)
    )

    constructor(httpUrl: String) : this(
            httpUrl,
            AemInstance.USER_DEFAULT,
            AemInstance.PASSWORD_DEFAULT,
            AemInstanceType.nameByUrl(httpUrl),
            debugPortByUrl(httpUrl)
    )

    override val environment: String
        get() = ENVIRONMENT

    override fun toString(): String {
        return "AemLocalInstance(httpUrl='$httpUrl', user='$user', password='$hiddenPassword', typeName='$typeName', debugPort=$debugPort)"
    }

}