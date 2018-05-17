package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.pkg.deploy.ListResponse
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import java.io.Serializable

class LocalInstance private constructor() : Instance, Serializable {

    override lateinit var httpUrl: String

    override val user: String = USER

    override lateinit var password: String

    override lateinit var typeName: String

    override lateinit var environment: String

    var debugPort: Int = 5005

    @get:JsonIgnore
    val jvmOptsDefaults: List<String>
        get() = mutableListOf<String>().apply {
            if (debugPort > 0) {
                add("-Xdebug")
                add("-Xrunjdwp:transport=dt_socket,address=$debugPort,server=y,suspend=n")
            }
            if (password != Instance.PASSWORD_DEFAULT) {
                add("-Dadmin.password=$password")
            }
        }

    @get:JsonIgnore
    var jvmOpts = mutableListOf(
            "-server", "-Xmx1024m", "-XX:MaxPermSize=256M", "-Djava.awt.headless=true"
    )

    @get:JsonProperty("jvmOpts")
    val jvmOptsString: String
        get() = (jvmOptsDefaults + jvmOpts).joinToString(" ")

    @get:JsonIgnore
    var startOpts = mutableListOf<String>()

    @get:JsonProperty("startOpts")
    val startOptsString: String
        get() = startOpts.joinToString(" ")

    @get:JsonIgnore
    val runModesDefault
        get() = listOf(typeName)

    @get:JsonIgnore
    var runModes = mutableListOf(ENVIRONMENT)

    @get:JsonProperty("runModes")
    val runModesString: String
        get() = (runModesDefault + runModes).joinToString(",")

    // TODO caching should be scoped per build, not per instance
    @Transient
    @get:JsonIgnore
    override var packages: ListResponse? = null

    override fun toString(): String {
        return "LocalInstance(httpUrl='$httpUrl', user='$user', password='$hiddenPassword', typeName='$typeName', debugPort=$debugPort)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocalInstance

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

        const val ENVIRONMENT = "local"

        const val USER = "admin"

        fun create(httpUrl: String, configurer: LocalInstance.() -> Unit): LocalInstance {
            return LocalInstance().apply {
                val instanceUrl = InstanceUrl.parse(httpUrl)
                if (instanceUrl.user != LocalInstance.USER) {
                    throw InstanceException("User '${instanceUrl.user}' (other than 'admin') is not allowed while using local instance(s).")
                }

                this.httpUrl = instanceUrl.httpUrl
                this.password = instanceUrl.password
                this.typeName = instanceUrl.typeName
                this.debugPort = instanceUrl.debugPort
                this.environment = ENVIRONMENT

                this.apply(configurer)
            }
        }

        fun create(httpUrl: String): LocalInstance {
            return create(httpUrl, {})
        }

    }

}