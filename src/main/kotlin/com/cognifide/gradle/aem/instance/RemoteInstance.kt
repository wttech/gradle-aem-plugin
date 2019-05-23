package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.Formats
import java.io.Serializable

class RemoteInstance private constructor(aem: AemExtension) : AbstractInstance(aem), Serializable {

    override lateinit var httpUrl: String

    override lateinit var user: String

    override lateinit var password: String

    override lateinit var typeName: String

    override lateinit var environment: String

    override fun toString(): String {
        return "RemoteInstance(httpUrl='$httpUrl', user='$user', password='${Formats.asPassword(password)}', environment='$environment', typeName='$typeName')"
    }

    companion object {

        fun create(aem: AemExtension, httpUrl: String, configurer: RemoteInstance.() -> Unit): RemoteInstance {
            return RemoteInstance(aem).apply {
                val instanceUrl = InstanceUrl.parse(httpUrl)

                this.httpUrl = instanceUrl.httpUrl
                this.user = instanceUrl.user
                this.password = instanceUrl.password
                this.typeName = instanceUrl.typeName

                this.environment = aem.env
                this.zoneId = aem.zoneId

                this.apply(configurer)
            }
        }

        fun create(aem: AemExtension, httpUrl: String): RemoteInstance {
            return create(aem, httpUrl) {}
        }
    }
}