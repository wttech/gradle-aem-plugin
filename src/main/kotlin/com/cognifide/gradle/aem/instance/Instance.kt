package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.api.AemException
import com.cognifide.gradle.aem.internal.Formats
import com.cognifide.gradle.aem.internal.Patterns
import com.cognifide.gradle.aem.pkg.deploy.ListResponse
import com.fasterxml.jackson.annotation.JsonIgnore
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

        fun properties(project: Project): List<Instance> {
            val localInstances = collectProperties(project, "local").map {
                val (name, props) = it
                val nameParts = name.split("-")
                if (nameParts.size != 2) {
                    throw InstanceException("Local instance name has invalid format: '$name'.")
                }
                val (environment, typeName) = nameParts
                val httpUrl = props["httpUrl"]
                        ?: throw InstanceException("Local instance named '$name' must have property 'httpUrl' defined.")

                LocalInstance.create(httpUrl, {
                    this.environment = environment
                    this.typeName = typeName

                    props["password"]?.let { this.password = it }
                    props["jvmOpts"]?.let { this.jvmOpts = it.split(" ") }
                    props["startOpts"]?.let { this.startOpts = it.split(" ") }
                    props["runModes"]?.let { this.runModes = it.split(",") }
                    props["debugPort"]?.let { this.debugPort = it.toInt() }
                })
            }.sortedBy { it.name }

            val remoteInstances = collectProperties(project, "remote").map {
                val (name, props) = it
                val nameParts = name.split("-")
                if (nameParts.size != 2) {
                    throw InstanceException("Remote instance name has invalid format: '$name'.")
                }
                val (environment, typeName) = nameParts
                val httpUrl = props["httpUrl"]
                        ?: throw InstanceException("Remote instance named '$name' must have property 'httpUrl' defined.")

                RemoteInstance.create(httpUrl, {
                    this.environment = environment
                    this.typeName = typeName

                    props["user"]?.let { this.user = it }
                    props["password"]?.let { this.password = it }

                })
            }.sortedBy { it.name }

            return localInstances + remoteInstances
        }

        private fun collectProperties(project: Project, type: String): MutableMap<String, MutableMap<String, String>> {
            return project.properties.filterKeys { Patterns.wildcard(it, "aem.instance.$type.*.*") }.entries.fold(mutableMapOf(), { result, e ->
                val (key, value) = e
                val parts = key.substringAfter(".$type.").split(".")
                if (parts.size != 2) {
                    throw InstanceException("Instance list property '$key' has invalid format.")
                }

                val (name, prop) = parts

                result.getOrPut(name, { mutableMapOf() })[prop] = value as String
                result
            })
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

    @get:JsonIgnore
    val httpBasicAuthUrl: String
        get() = httpUrl // TODO inject user and password, encode password special characters

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

fun Instance.isInitialized(project: Project): Boolean {
    return this !is LocalInstance || LocalHandle(project, this).initialized
}

fun Instance.isBeingInitialized(project: Project): Boolean {
    return this is LocalInstance && !LocalHandle(project, this).initialized
}