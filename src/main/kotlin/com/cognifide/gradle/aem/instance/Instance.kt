package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.base.api.AemConfig
import com.cognifide.gradle.aem.base.api.AemException
import com.cognifide.gradle.aem.internal.Formats
import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.pkg.deploy.ListResponse
import com.fasterxml.jackson.annotation.JsonIgnore
import org.gradle.api.Project
import java.io.Serializable
import java.net.MalformedURLException
import java.net.URL
import kotlin.reflect.KClass

interface Instance : Serializable {

    companion object {

        val FILTER_ANY = PropertyParser.FILTER_DEFAULT

        val FILTER_LOCAL = "local-*"

        val FILTER_AUTHOR = "*-author"

        val ENVIRONMENT_CMD = "cmd"

        val URL_AUTHOR_DEFAULT = "http://localhost:4502"

        val URL_PUBLISH_DEFAULT = "http://localhost:4503"

        val USER_DEFAULT = "admin"

        val PASSWORD_DEFAULT = "admin"

        val LIST_PROP = "aem.deploy.instance.list"

        val NAME_PROP = "aem.deploy.instance.name"

        fun parse(str: String): List<Instance> {
            return str.split(";").map { urlRaw ->
                val parts = urlRaw.split(",")

                when (parts.size) {
                    4 -> {
                        val (httpUrl, type, user, password) = parts

                        RemoteInstance(httpUrl, user, password, type, ENVIRONMENT_CMD)
                    }
                    3 -> {
                        val (httpUrl, user, password) = parts
                        val type = InstanceType.byUrl(httpUrl).name.toLowerCase()

                        RemoteInstance(httpUrl, user, password, type, ENVIRONMENT_CMD)
                    }
                    else -> {
                        try {
                            val urlObj = URL(urlRaw)
                            val userInfo = urlObj.userInfo ?: "$USER_DEFAULT:$PASSWORD_DEFAULT"
                            val userParts = userInfo.split(":")
                            val (user, password) = when (userParts.size) {
                                2 -> userParts
                                else -> throw AemException("Instance URL '$urlRaw' must have both user and password specified.")
                            }
                            val httpUrl = "${urlObj.protocol}://${urlObj.host}:${urlObj.port}"
                            val type = InstanceType.byUrl(httpUrl).name.toLowerCase()

                            RemoteInstance(httpUrl, user, password, type, ENVIRONMENT_CMD)
                        } catch (e: MalformedURLException) {
                            throw AemException("Cannot parse instance URL: '$urlRaw'", e)
                        }
                    }
                }
            }
        }

        fun defaults(): List<Instance> {
            return listOf(
                    LocalInstance(URL_AUTHOR_DEFAULT),
                    LocalInstance(URL_PUBLISH_DEFAULT)
            )
        }

        fun filter(project: Project, instanceFilter: String = FILTER_LOCAL): List<Instance> {
            val config = AemConfig.of(project)
            val instanceValues = project.properties[LIST_PROP] as String?

            // Specified directly should not be filtered
            if (!instanceValues.isNullOrBlank()) {
                return parse(instanceValues!!)
            }

            // Predefined and defaults are filterable
            val instances = if (!config.instances.values.isEmpty()) {
                config.instances.values
            } else {
                defaults()
            }

            return instances.filter { instance ->
                PropertyParser(project).filter(instance.name, NAME_PROP, instanceFilter)
            }
        }

        @Suppress("unchecked_cast")
        fun <T : Instance> filter(project: Project, type: KClass<T>): List<T> {
            return filter(project, Instance.FILTER_LOCAL).filterIsInstance(type.java)
        }

        fun locals(project: Project): List<LocalInstance> {
            return filter(project, LocalInstance::class)
        }

        fun remotes(project: Project): List<RemoteInstance> {
            return filter(project, RemoteInstance::class)
        }

        fun portOfUrl(url: String): Int {
            return URL(url).port
        }

    }

    val httpUrl: String

    val httpPort: Int
        get() = portOfUrl(httpUrl)

    val user: String

    val password: String

    @get:JsonIgnore
    val hiddenPassword: String
        get() = "*".repeat(password.length)

    val environment: String

    val typeName: String

    val type: InstanceType
        get() = InstanceType.byName(typeName)

    @get:JsonIgnore
    val credentials: String
        get() = "$user:$password"

    val name: String
        get() = "$environment-$typeName"

    fun validate() {
        if (!Formats.URL_VALIDATOR.isValid(httpUrl)) {
            throw AemException("Malformed URL address detected in $this")
        }

        if (user.isBlank()) {
            throw AemException("User cannot be blank in $this")
        }

        if (password.isBlank()) {
            throw AemException("Password cannot be blank in $this")
        }

        if (environment.isBlank()) {
            throw AemException("Environment cannot be blank in $this")
        }

        if (typeName.isBlank()) {
            throw AemException("Type cannot be blank in $this")
        }
    }

    @get:JsonIgnore
    var packages: ListResponse?

}