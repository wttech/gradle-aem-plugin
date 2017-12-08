package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.deploy.ListResponse
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.Serializable

class LocalInstance(
        override val httpUrl: String,
        override val user: String,
        override val password: String,
        override val typeName: String,
        val debugPort: Int,
        val jvmOpts: List<String>,
        val startOpts: List<String>
) : Instance, Serializable {

    companion object {
        val ENVIRONMENT = "local"

        fun debugPortByUrl(url: String): Int {
            return "1${Instance.portOfUrl(url)}".toInt()
        }
    }

    constructor(httpUrl: String, user: String, password: String, type: String, debugPort: Int) : this(
            httpUrl,
            user,
            password,
            type,
            debugPort,
            listOf(),
            listOf()
    )

    constructor(httpUrl: String, user: String, password: String) : this(
            httpUrl,
            user,
            password,
            InstanceType.byUrl(httpUrl).name,
            debugPortByUrl(httpUrl),
            listOf(),
            listOf()
    )

    constructor(httpUrl: String, password: String) : this(
            httpUrl,
            Instance.USER_DEFAULT,
            password
    )

    constructor(httpUrl: String) : this(
            httpUrl,
            Instance.USER_DEFAULT,
            Instance.PASSWORD_DEFAULT,
            InstanceType.nameByUrl(httpUrl),
            debugPortByUrl(httpUrl),
            listOf(),
            listOf()
    )

    @get:JsonIgnore
    val jvmOpt: String
        get() = jvmOpts.joinToString(" ")

    @get:JsonIgnore
    val startOpt: String
        get() = startOpts.joinToString(" ")

    override val environment: String
        get() = ENVIRONMENT

    override fun toString(): String {
        return "LocalInstance(httpUrl='$httpUrl', user='$user', password='$hiddenPassword', type='$typeName', debugPort=$debugPort)"
    }

    @Transient
    @get:JsonIgnore
    override var packages: ListResponse? = null

}