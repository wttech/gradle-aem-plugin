package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.base.api.AemConfig
import com.cognifide.gradle.aem.base.api.AemException
import com.cognifide.gradle.aem.internal.Formats
import com.cognifide.gradle.aem.internal.Patterns
import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.pkg.deploy.ListResponse
import com.fasterxml.jackson.annotation.JsonIgnore
import org.gradle.api.Project
import java.io.Serializable
import java.net.URL
import kotlin.reflect.KClass

interface Instance : Serializable {

    companion object {

        val FILTER_ANY = PropertyParser.FILTER_DEFAULT

        val FILTER_AUTHOR = "*-author"

        val ENVIRONMENT_CMD = "cmd"

        val URL_AUTHOR_DEFAULT = "http://localhost:4502"

        val URL_PUBLISH_DEFAULT = "http://localhost:4503"

        val USER_DEFAULT = "admin"

        val PASSWORD_DEFAULT = "admin"

        val LIST_PROP = "aem.instance.list"

        val NAME_PROP = "aem.instance.name"

        val AUTHOR_URL_PROP = "aem.instance.author.httpUrl"

        val PUBLISH_URL_PROP = "aem.instance.publish.httpUrl"

        val AUTHORS_FILTER_PROP = "aem.instance.authors"

        val PUBLISHERS_PROP = "aem.instance.publishers"

        val LOCAL_URLS = listOf(
                "http://localhost",
                "https://localhost",
                "http://127.0.0.1",
                "https://127.0.0.1"
        )

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
                        val type = InstanceType.nameByUrl(httpUrl)

                        RemoteInstance(httpUrl, user, password, type, ENVIRONMENT_CMD)
                    }
                    else -> {
                        RemoteInstance.create(urlRaw, ENVIRONMENT_CMD)
                    }
                }
            }
        }

        fun defaults(project: Project): List<Instance> {
            val config = AemConfig.of(project)
            val authorUrl = project.properties.getOrElse(AUTHOR_URL_PROP, { URL_AUTHOR_DEFAULT }) as String
            val publishUrl = project.properties.getOrElse(PUBLISH_URL_PROP, { URL_PUBLISH_DEFAULT }) as String

            return listOf(
                    RemoteInstance.create(authorUrl, config.deployEnvironment),
                    RemoteInstance.create(publishUrl, config.deployEnvironment)
            )
        }

        fun filter(project: Project): List<Instance> {
            return filter(project, AemConfig.of(project).deployInstanceName)
        }

        fun filter(project: Project, instanceFilter: String): List<Instance> {
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
                defaults(project)
            }

            // Handle name pattern filtering
            return instances.filter { instance ->
                when {
                    config.propParser.flag(AUTHORS_FILTER_PROP) -> {
                        Patterns.wildcard(instance.name, "${config.deployEnvironment}-${InstanceType.AUTHOR}")
                    }
                    config.propParser.flag(PUBLISHERS_PROP) -> {
                        Patterns.wildcard(instance.name, "${config.deployEnvironment}-${InstanceType.PUBLISH}")
                    }
                    else -> config.propParser.filter(instance.name, NAME_PROP, instanceFilter)
                }
            }
        }

        fun <T : Instance> filter(project: Project, type: KClass<T>): List<T> {
            return filter(project).filterIsInstance(type.java)
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