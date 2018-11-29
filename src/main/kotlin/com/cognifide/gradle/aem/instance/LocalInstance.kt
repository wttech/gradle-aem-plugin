package com.cognifide.gradle.aem.instance

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable
import org.gradle.api.Project

class LocalInstance private constructor(project: Project) : AbstractInstance(project), Serializable {

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
    var jvmOpts: List<String> = listOf(
            "-server", "-Xmx1024m", "-XX:MaxPermSize=256M", "-Djava.awt.headless=true"
    )

    @get:JsonProperty("jvmOpts")
    val jvmOptsString: String
        get() = (jvmOptsDefaults + jvmOpts).joinToString(" ")

    @get:JsonIgnore
    var startOpts: List<String> = listOf()

    @get:JsonProperty("startOpts")
    val startOptsString: String
        get() = startOpts.joinToString(" ")

    @get:JsonIgnore
    val runModesDefault
        get() = listOf(type.name.toLowerCase())

    @get:JsonIgnore
    var runModes: List<String> = listOf(ENVIRONMENT)

    @get:JsonProperty("runModes")
    val runModesString: String
        get() = (runModesDefault + runModes).joinToString(",")

    override fun toString(): String {
        return "LocalInstance(httpUrl='$httpUrl', user='$user', password='$hiddenPassword', typeName='$typeName', debugPort=$debugPort)"
    }

    companion object {

        const val ENVIRONMENT = "local"

        const val USER = "admin"

        fun create(project: Project, httpUrl: String, configurer: LocalInstance.() -> Unit): LocalInstance {
            return LocalInstance(project).apply {
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

        fun create(project: Project, httpUrl: String): LocalInstance {
            return create(project, httpUrl) {}
        }
    }
}