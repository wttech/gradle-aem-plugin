package com.cognifide.gradle.aem.instance

import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.gradle.api.Project
import java.io.Serializable

class RemoteInstance private constructor(@Transient
                                         @JsonIgnore
                                         private val project: Project) : Instance, Serializable {

    override lateinit var httpUrl: String

    override lateinit var user: String

    override lateinit var password: String

    override lateinit var typeName: String

    override lateinit var environment: String

    override fun sync(synchronizer: (InstanceSync) -> Unit) {
        synchronizer(InstanceSync(project, this))
    }

    override fun toString(): String {
        return "RemoteInstance(httpUrl='$httpUrl', user='$user', password='$hiddenPassword', environment='$environment', typeName='$typeName')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RemoteInstance

        return EqualsBuilder()
                .append(name, other.name)
                .append(httpUrl, other.httpUrl)
                .isEquals
    }

    override fun hashCode(): Int {
        return HashCodeBuilder()
                .append(name)
                .append(httpUrl)
                .toHashCode()
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