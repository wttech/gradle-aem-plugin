package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.api.AemException
import com.cognifide.gradle.aem.internal.Formats
import com.cognifide.gradle.aem.internal.Patterns
import com.cognifide.gradle.aem.pkg.deploy.ListResponse
import com.fasterxml.jackson.annotation.JsonIgnore
import org.gradle.api.Project
import java.io.Serializable
import java.net.URL
import kotlin.reflect.KClass

interface Instance : Serializable {

    companion object {

        val FILTER_ANY = "*"

        val ENVIRONMENT_CMD = "cmd"

        val URL_AUTHOR_DEFAULT = "http://localhost:4502"

        val URL_PUBLISH_DEFAULT = "http://localhost:4503"

        val USER_DEFAULT = "admin"

        val PASSWORD_DEFAULT = "admin"

        val AUTHOR_URL_PROP = "aem.instance.author.httpUrl"

        val PUBLISH_URL_PROP = "aem.instance.publish.httpUrl"

        val AUTHORS_PROP = "aem.instance.authors"

        val PUBLISHERS_PROP = "aem.instance.publishers"

        fun parse(str: String): List<Instance> {
            return str.split(";").map { urlRaw ->
                val parts = urlRaw.split(",")

                when (parts.size) {
                    4 -> {
                        val (httpUrl, type, user, password) = parts

                        RemoteInstance.create(httpUrl, {
                            this.user = user
                            this.password = password
                            this.typeName = type
                        })
                    }
                    3 -> {
                        val (httpUrl, user, password) = parts

                        RemoteInstance.create(httpUrl, {
                            this.user = user
                            this.password = password
                        })
                    }
                    else -> {
                        RemoteInstance.create(urlRaw)
                    }
                }
            }
        }

        fun defaults(project: Project): List<Instance> {
            val config = AemConfig.of(project)
            val authorUrl = project.properties.getOrElse(AUTHOR_URL_PROP, { URL_AUTHOR_DEFAULT }) as String
            val publishUrl = project.properties.getOrElse(PUBLISH_URL_PROP, { URL_PUBLISH_DEFAULT }) as String

            return listOf(
                    RemoteInstance.create(authorUrl, { environment = config.deployEnvironment }),
                    RemoteInstance.create(publishUrl, { environment = config.deployEnvironment })
            )
        }

        fun filter(project: Project): List<Instance> {
            return filter(project, AemConfig.of(project).deployInstanceName)
        }

        fun filter(project: Project, instanceFilter: String): List<Instance> {
            val config = AemConfig.of(project)

            // Specified directly should not be filtered
            if (config.deployInstanceList.isNotBlank()) {
                return parse(config.deployInstanceList)
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
                    config.propParser.flag(AUTHORS_PROP) -> {
                        Patterns.wildcard(instance.name, "${config.deployEnvironment}-${InstanceType.AUTHOR}*")
                    }
                    config.propParser.flag(PUBLISHERS_PROP) -> {
                        Patterns.wildcard(instance.name, "${config.deployEnvironment}-${InstanceType.PUBLISH}*")
                    }
                    else -> Patterns.wildcards(instance.name, instanceFilter)
                }
            }
        }

        fun <T : Instance> filter(project: Project, type: KClass<T>): List<T> {
            return filter(project).filterIsInstance(type.java)
        }

        fun locals(project: Project): List<LocalInstance> {
            return filter(project, LocalInstance::class)
        }

        fun handles(project: Project): List<LocalHandle> {
            return Instance.locals(project).map { LocalHandle(project, it) }
        }

        fun remotes(project: Project): List<RemoteInstance> {
            return filter(project, RemoteInstance::class)
        }

    }

    val httpUrl: String

    val httpPort: Int
        get() = InstanceUrl.parse(httpUrl).httpPort

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