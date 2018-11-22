package com.cognifide.gradle.aem.instance

import java.io.Serializable
import org.gradle.api.Project

class RemoteInstance private constructor(project: Project) : AbstractInstance(project), Serializable {

    override lateinit var httpUrl: String

    override lateinit var user: String

    override lateinit var password: String

    override lateinit var typeName: String

    override lateinit var environment: String

    override fun toString(): String {
        return "RemoteInstance(httpUrl='$httpUrl', user='$user', password='$hiddenPassword', environment='$environment', typeName='$typeName')"
    }

    companion object {

        fun create(project: Project, httpUrl: String, configurer: RemoteInstance.() -> Unit): RemoteInstance {
            return RemoteInstance(project).apply {
                val instanceUrl = InstanceUrl.parse(httpUrl)

                this.httpUrl = instanceUrl.httpUrl
                this.user = instanceUrl.user
                this.password = instanceUrl.password
                this.typeName = instanceUrl.typeName
                this.environment = Instance.ENVIRONMENT_CMD

                this.apply(configurer)
            }
        }

        fun create(project: Project, httpUrl: String): RemoteInstance {
            return create(project, httpUrl) {}
        }
    }
}