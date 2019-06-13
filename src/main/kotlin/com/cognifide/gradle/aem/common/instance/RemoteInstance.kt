package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.utils.Formats
import java.io.Serializable

class RemoteInstance private constructor(aem: AemExtension) : AbstractInstance(aem), Serializable {

    override lateinit var user: String

    override lateinit var password: String

    override fun toString(): String {
        return "RemoteInstance(httpUrl='$httpUrl', user='$user', password='${Formats.asPassword(password)}', environment='$environment', id='$id')"
    }

    companion object {

        fun create(aem: AemExtension, httpUrl: String, configurer: RemoteInstance.() -> Unit): RemoteInstance {
            return RemoteInstance(aem).apply {
                val instanceUrl = InstanceUrl.parse(httpUrl)

                this.httpUrl = instanceUrl.httpUrl
                this.user = instanceUrl.user
                this.password = instanceUrl.password
                this.environment = aem.env
                this.id = instanceUrl.id

                this.apply(configurer)
            }
        }

        fun create(aem: AemExtension, httpUrl: String): RemoteInstance {
            return create(aem, httpUrl) {}
        }
    }
}