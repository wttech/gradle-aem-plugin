package com.cognifide.gradle.aem.instance

import java.io.Serializable

class LocalInstance(
        override val httpUrl: String,
        override val user: String,
        override val password: String,
        override val typeName: String,
        val debugPort: Int
) : Instance, Serializable {

    companion object {
        val ENVIRONMENT = "local"

        fun debugPortByUrl(url: String): Int {
            return "1${Instance.portOfUrl(url)}".toInt()
        }
    }

    constructor(httpUrl: String, user: String, password: String) : this(
            httpUrl,
            user,
            password,
            InstanceType.byUrl(httpUrl).name,
            debugPortByUrl(httpUrl)
    )

    constructor(httpUrl: String) : this(
            httpUrl,
            Instance.USER_DEFAULT,
            Instance.PASSWORD_DEFAULT,
            InstanceType.nameByUrl(httpUrl),
            debugPortByUrl(httpUrl)
    )

    override val environment: String
        get() = ENVIRONMENT

    override fun toString(): String {
        return "LocalInstance(httpUrl='$httpUrl', user='$user', password='$hiddenPassword', type='$typeName', debugPort=$debugPort)"
    }

}