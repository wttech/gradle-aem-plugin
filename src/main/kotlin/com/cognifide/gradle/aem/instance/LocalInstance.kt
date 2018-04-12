package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.pkg.deploy.ListResponse
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.Serializable

class LocalInstance private constructor() : Instance, Serializable {

    override lateinit var httpUrl: String

    override lateinit var user: String

    override lateinit var password: String

    override lateinit var typeName: String

    var debugPort: Int = 5005

    override val environment: String
        get() = ENVIRONMENT

    @Transient
    @get:JsonIgnore
    override var packages: ListResponse? = null

    override fun toString(): String {
        return "LocalInstance(httpUrl='$httpUrl', user='$user', password='$hiddenPassword', typeName='$typeName', debugPort=$debugPort)"
    }

    companion object {

        const val ENVIRONMENT = "local"

        fun create(httpUrl: String, configurer: LocalInstance.() -> Unit): LocalInstance {
            return LocalInstance().apply {
                val instanceUrl = InstanceUrl.parse(httpUrl)

                this.httpUrl = instanceUrl.httpUrl
                this.user = instanceUrl.user
                this.password = instanceUrl.password
                this.typeName = instanceUrl.typeName
                this.debugPort = instanceUrl.debugPort

                this.apply(configurer)
            }
        }

        fun create(httpUrl: String): LocalInstance {
            return create(httpUrl, {})
        }

    }

}