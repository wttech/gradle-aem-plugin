package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemException
import com.cognifide.gradle.aem.api.AemExtension
import com.cognifide.gradle.aem.internal.Formats
import com.cognifide.gradle.aem.internal.Patterns
import com.fasterxml.jackson.annotation.JsonIgnore
import org.gradle.api.Project
import java.io.Serializable
import kotlin.reflect.KClass

interface Instance : Serializable {

    val httpUrl: String

    val httpPort: Int
        get() = InstanceUrl.parse(httpUrl).httpPort

    @get:JsonIgnore
    val httpBasicAuthUrl: String
        get() = InstanceUrl.parse(httpUrl).httpBasicAuthUrl(user, password)

    val user: String

    val password: String

    @get:JsonIgnore
    val hiddenPassword: String
        get() = "*".repeat(password.length)

    val environment: String

    @get:JsonIgnore
    val cmd: Boolean
        get() = environment == ENVIRONMENT_CMD

    val typeName: String

    val type: InstanceType
        get() = InstanceType.byName(typeName)

    @get:JsonIgnore
    val credentials: String
        get() = "$user:$password"

    val name: String
        get() = "$environment-$typeName"

    fun sync(synchronizer: (InstanceSync) -> Unit)

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

    companion object {

        const val FILTER_ANY = "*"

        const val ENVIRONMENT_CMD = "cmd"

        const val URL_AUTHOR_DEFAULT = "http://localhost:4502"

        const val URL_PUBLISH_DEFAULT = "http://localhost:4503"

        const val USER_DEFAULT = "admin"

        const val PASSWORD_DEFAULT = "admin"

        const val AUTHORS_PROP = "aem.instance.authors"

        const val PUBLISHERS_PROP = "aem.instance.publishers"

        fun parse(project: Project, str: String): List<RemoteInstance> {
            return Formats.toList(str).map { RemoteInstance.create(project, it) }
        }

        fun properties(project: Project): List<Instance> {
            val localInstances = collectProperties(project, "local").map { e ->
                val (name, props) = e
                val nameParts = name.split("-")
                if (nameParts.size != 2) {
                    throw InstanceException("Local instance name has invalid format: '$name'.")
                }
                val (environment, typeName) = nameParts
                val httpUrl = props["httpUrl"]
                        ?: throw InstanceException("Local instance named '$name' must have property 'httpUrl' defined.")

                LocalInstance.create(project, httpUrl) {
                    this.environment = environment
                    this.typeName = typeName

                    props["password"]?.let { this.password = it }
                    props["jvmOpts"]?.let { this.jvmOpts = it.split(" ") }
                    props["startOpts"]?.let { this.startOpts = it.split(" ") }
                    props["runModes"]?.let { this.runModes = it.split(",") }
                    props["debugPort"]?.let { this.debugPort = it.toInt() }
                }
            }.sortedBy { it.name }

            val remoteInstances = collectProperties(project, "remote").map { e ->
                val (name, props) = e
                val nameParts = name.split("-")
                if (nameParts.size != 2) {
                    throw InstanceException("Remote instance name has invalid format: '$name'.")
                }
                val (environment, typeName) = nameParts
                val httpUrl = props["httpUrl"]
                        ?: throw InstanceException("Remote instance named '$name' must have property 'httpUrl' defined.")

                RemoteInstance.create(project, httpUrl) {
                    this.environment = environment
                    this.typeName = typeName

                    props["user"]?.let { this.user = it }
                    props["password"]?.let { this.password = it }

                }
            }.sortedBy { it.name }

            return localInstances + remoteInstances
        }

        private fun collectProperties(project: Project, type: String): MutableMap<String, MutableMap<String, String>> {
            return project.properties.filterKeys { Patterns.wildcard(it, "aem.instance.$type.*.*") }.entries.fold(mutableMapOf()) { result, e ->
                val (key, value) = e
                val parts = key.substringAfter(".$type.").split(".")
                if (parts.size != 2) {
                    throw InstanceException("Instance list property '$key' has invalid format.")
                }

                val (name, prop) = parts

                result.getOrPut(name) { mutableMapOf() }[prop] = value as String
                result
            }
        }

        fun defaults(project: Project): List<RemoteInstance> {
            val config = AemExtension.of(project).config

            return listOf(
                    RemoteInstance.create(project, URL_AUTHOR_DEFAULT) { environment = config.environment },
                    RemoteInstance.create(project, URL_PUBLISH_DEFAULT) { environment = config.environment }
            )
        }

        fun filter(project: Project): List<Instance> {
            return filter(project, AemExtension.of(project).config.instanceName)
        }

        fun filter(project: Project, instanceFilter: String): List<Instance> {
            val aem = AemExtension.of(project)
            val all = aem.config.instances.values

            // Specified by command line should not be filtered
            val cmd = all.filter { it.environment == Instance.ENVIRONMENT_CMD }
            if (cmd.isNotEmpty()) {
                return cmd
            }

            // Defined by build script, via properties or defaults are filterable by name
            return all.filter { instance ->
                when {
                    aem.props.flag(AUTHORS_PROP) -> {
                        Patterns.wildcard(instance.name, "${aem.config.environment}-${InstanceType.AUTHOR}*")
                    }
                    aem.props.flag(PUBLISHERS_PROP) -> {
                        Patterns.wildcard(instance.name, "${aem.config.environment}-${InstanceType.PUBLISH}*")
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

        fun any(project: Project): Instance {
            val aem = AemExtension.of(project)

            val cmdInstanceArg = aem.props.string("aem.instance")
            if (!cmdInstanceArg.isNullOrBlank()) {
                val cmdInstance = aem.config.parseInstance(cmdInstanceArg)

                aem.logger.info("Using instance specified by command line parameter: $cmdInstance")
                return cmdInstance
            }

            val namedInstance = Instance.filter(project, aem.config.instanceName).firstOrNull()
            if (namedInstance != null) {
                aem.logger.info("Using first instance matching filter '${aem.config.instanceName}': $namedInstance")
                return namedInstance
            }

            val anyInstance = Instance.filter(project, FILTER_ANY).firstOrNull()
            if (anyInstance != null) {
                aem.logger.info("Using first instance matching filter '$FILTER_ANY': $anyInstance")
                return anyInstance
            }

            throw InstanceException("Single instance cannot be determined neither by command line parameter nor AEM config.")
        }

        fun concrete(project: Project, type: String): Instance? {
            val aem = AemExtension.of(project)

            return aem.props.prop("aem.instance.$type")?.run {
                aem.config.parseInstance(this)
            }
        }
    }
}

val Collection<Instance>.names: String
    get() = joinToString(", ") { it.name }

fun Collection<Instance>.toLocalHandles(project: Project): List<LocalHandle> {
    return filterIsInstance(LocalInstance::class.java).map { LocalHandle(project, it) }
}

fun Instance.isInitialized(project: Project): Boolean {
    return this !is LocalInstance || LocalHandle(project, this).initialized
}

fun Instance.isBeingInitialized(project: Project): Boolean {
    return this is LocalInstance && !LocalHandle(project, this).initialized
}