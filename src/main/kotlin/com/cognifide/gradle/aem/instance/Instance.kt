package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.api.AemException
import com.cognifide.gradle.aem.internal.Formats
import com.cognifide.gradle.aem.internal.Patterns
import com.cognifide.gradle.aem.pkg.deploy.ListResponse
import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Project
import java.io.Serializable
import kotlin.reflect.KClass

interface Instance : Serializable {

    companion object {

        val FILTER_ANY = "*"

        val ENVIRONMENT_CMD = "cmd"

        val URL_AUTHOR_DEFAULT = "http://localhost:4502"

        val URL_PUBLISH_DEFAULT = "http://localhost:4503"

        val USER_DEFAULT = "admin"

        val PASSWORD_DEFAULT = "admin"

        val AUTHORS_PROP = "aem.instance.authors"

        val PUBLISHERS_PROP = "aem.instance.publishers"

        val REMOTE_PROP_PATTERN = "aem.instance.remote.*.httpUrl"

        fun parse(str: String): List<RemoteInstance> {
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

        fun properties(project: Project): List<RemoteInstance> {
            return project.properties.filterKeys { Patterns.wildcard(it, REMOTE_PROP_PATTERN) }.map {
                val nameParts = StringUtils.substringBetween(it.key, ".remote.", ".httpUrl")!!.split("-")
                if (nameParts.size != 2) {
                    throw InstanceException("Instance list property '${it.key}' does not have valid name part.")
                }

                val (environment, typeName) = nameParts
                val httpUrl = it.value as String?
                if (httpUrl.isNullOrBlank()) {
                    throw InstanceException("Instance list property '${it.key}' value cannot be blank.")
                }

                RemoteInstance.create(httpUrl!!, {
                    this.environment = environment
                    this.typeName = typeName
                })
            }.sortedBy { it.name }
        }

        fun defaults(project: Project): List<RemoteInstance> {
            val config = AemConfig.of(project)

            return listOf(
                    RemoteInstance.create(URL_AUTHOR_DEFAULT, { environment = config.environment }),
                    RemoteInstance.create(URL_PUBLISH_DEFAULT, { environment = config.environment })
            )
        }

        fun filter(project: Project): List<Instance> {
            return filter(project, AemConfig.of(project).instanceName)
        }

        fun filter(project: Project, instanceFilter: String): List<Instance> {
            val config = AemConfig.of(project)
            val all = config.instances.values

            // Specified by command line should not be filtered
            val cmd = all.filter { it.environment == Instance.ENVIRONMENT_CMD }
            if (cmd.isNotEmpty()) {
                return cmd
            }

            // Defined by build script, via properties or defaults are filterable by name
            return all.filter { instance ->
                when {
                    config.props.flag(AUTHORS_PROP) -> {
                        Patterns.wildcard(instance.name, "${config.environment}-${InstanceType.AUTHOR}*")
                    }
                    config.props.flag(PUBLISHERS_PROP) -> {
                        Patterns.wildcard(instance.name, "${config.environment}-${InstanceType.PUBLISH}*")
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

val Collection<Instance>.names: String
    get() = joinToString(", ") { it.name }