package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.pkg.deploy.ListResponse
import com.fasterxml.jackson.annotation.JsonIgnore
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

        fun create(httpUrl: String, user: String, password: String): Instance {
            return LocalInstance(
                    httpUrl,
                    user,
                    password,
                    InstanceType.nameByUrl(httpUrl),
                    debugPortByUrl(httpUrl)
            )
        }

        fun create(httpUrl: String, type: String): Instance {
            val instanceUrl = InstanceUrl.parse(httpUrl)

            return LocalInstance(
                    instanceUrl.httpUrl,
                    instanceUrl.user,
                    instanceUrl.password,
                    type,
                    debugPortByUrl(httpUrl)
            )
        }

        fun create(httpUrl: String): LocalInstance {
            val instanceUrl = InstanceUrl.parse(httpUrl)

            return LocalInstance(
                    instanceUrl.httpUrl,
                    instanceUrl.user,
                    instanceUrl.password,
                    InstanceType.nameByUrl(httpUrl),
                    debugPortByUrl(httpUrl)
            )
        }

        fun create(httpUrl: String, user: String, password: String, type: String): LocalInstance {
            return LocalInstance(
                    httpUrl,
                    user,
                    password,
                    type,
                    debugPortByUrl(httpUrl)
            )
        }
    }

    override val environment: String
        get() = ENVIRONMENT

    override fun toString(): String {
        return "LocalInstance(httpUrl='$httpUrl', user='$user', password='$hiddenPassword', type='$typeName', debugPort=$debugPort)"
    }

    @Transient
    @get:JsonIgnore
    override var packages: ListResponse? = null


}