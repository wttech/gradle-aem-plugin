package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemException
import com.cognifide.gradle.aem.internal.Formats
import com.cognifide.gradle.aem.internal.Patterns
import com.fasterxml.jackson.annotation.JsonIgnore
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import java.io.Serializable

interface Instance : Serializable {

    @get:Input
    val httpUrl: String

    @get:Internal
    val httpPort: Int
        get() = InstanceUrl.parse(httpUrl).httpPort

    @get:Internal
    @get:JsonIgnore
    val httpBasicAuthUrl: String
        get() = InstanceUrl.parse(httpUrl).httpBasicAuthUrl(user, password)

    @get:Input
    val user: String

    @get:Input
    val password: String

    @get:Internal
    @get:JsonIgnore
    val hiddenPassword: String
        get() = "*".repeat(password.length)

    @get:Input
    val environment: String

    @get:Internal
    @get:JsonIgnore
    val cmd: Boolean
        get() = environment == ENVIRONMENT_CMD

    @get:Input
    val typeName: String

    @get:Internal
    val type: InstanceType
        get() = InstanceType.byName(typeName)

    @get:Internal
    @get:JsonIgnore
    val credentials: String
        get() = "$user:$password"

    @get:Internal
    val name: String
        get() = "$environment-$typeName"

    @get:Internal
    @get:JsonIgnore
    val sync: InstanceSync

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

        fun defaults(project: Project, environment: String): List<RemoteInstance> {
            return listOf(
                    RemoteInstance.create(project, URL_AUTHOR_DEFAULT) { this.environment = environment },
                    RemoteInstance.create(project, URL_PUBLISH_DEFAULT) { this.environment = environment }
            )
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