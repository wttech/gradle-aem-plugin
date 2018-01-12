package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.pkg.deploy.ListResponse
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.Serializable

class RemoteInstance(
        override val httpUrl: String,
        override val user: String,
        override val password: String,
        override val typeName: String,
        override val environment: String
) : Instance, Serializable {

    companion object {
        fun create(httpUrl: String): RemoteInstance {
            return create(httpUrl, LocalInstance.ENVIRONMENT)
        }

        fun create(httpUrl: String, environment: String): RemoteInstance {
            val instanceUrl = InstanceUrl.parse(httpUrl)

            return RemoteInstance(
                    instanceUrl.httpUrl,
                    instanceUrl.user,
                    instanceUrl.password,
                    InstanceType.byUrl(httpUrl).name.toLowerCase(),
                    environment
            )
        }

        fun create(httpUrl: String, user: String, password: String, environment: String): RemoteInstance {
            return RemoteInstance(
                    httpUrl,
                    user,
                    password,
                    InstanceType.byUrl(httpUrl).name.toLowerCase(),
                    environment
            )
        }
    }

    override fun toString(): String {
        return "RemoteInstance(httpUrl='$httpUrl', user='$user', password='$hiddenPassword', type='$typeName', environment='$environment')"
    }

    @Transient
    @get:JsonIgnore
    override var packages: ListResponse? = null

}