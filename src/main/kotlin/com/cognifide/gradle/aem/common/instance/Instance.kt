package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.utils.Formats
import com.cognifide.gradle.aem.common.utils.Patterns
import com.cognifide.gradle.aem.common.utils.formats.JsonPassword
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.io.Serializable
import java.time.ZoneId
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

interface Instance : Serializable {

    @get:Input
    val httpUrl: String

    @get:Internal
    val httpPort: Int
        get() = InstanceUrl.parse(httpUrl).httpPort

    @get:Internal
    @get:JsonIgnore
    val httpBasicAuthUrl: String
        get() = InstanceUrl.parse(httpUrl).basicAuth(user, password)

    @get:Input
    val user: String

    @get:Input
    @get:JsonSerialize(using = JsonPassword::class, `as` = String::class)
    val password: String

    @get:Internal
    @get:JsonIgnore
    val hiddenPassword: String
        get() = "*".repeat(password.length)

    @get:Input
    val name: String

    @get:Input
    val environment: String

    @get:Input
    val id: String

    @get:Internal
    @get:JsonIgnore
    val cmd: Boolean
        get() = environment == ENVIRONMENT_CMD

    @get:Internal
    val type: IdType
        get() = IdType.byId(id)

    @get:Internal
    @get:JsonIgnore
    val credentials: String
        get() = "$user:$password"

    @get:Internal
    @get:JsonIgnore
    val available: Boolean

    @get:Internal
    @get:JsonIgnore
    val zoneId: ZoneId

    @get:Input
    val properties: Map<String, String?>

    fun property(key: String, value: String?)

    fun property(key: String): String?

    @get:JsonIgnore
    val systemProperties: Map<String, String>

    @get:JsonIgnore
    val version: String

    @get:Internal
    @get:JsonIgnore
    val sync: InstanceSync

    fun <T> sync(synchronizer: InstanceSync.() -> T): T {
        return sync.run(synchronizer)
    }

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

        if (id.isBlank()) {
            throw AemException("Type name cannot be blank in $this")
        }
    }

    @get:Internal
    @get:JsonIgnore
    val json: String
        get() = Formats.toJson(this)

    companion object {

        const val FILTER_ANY = "*"

        const val ENVIRONMENT_CMD = "cmd"

        const val URL_AUTHOR_DEFAULT = "http://localhost:4502"

        const val URL_PUBLISH_DEFAULT = "http://localhost:4503"

        const val USER_DEFAULT = "admin"

        const val PASSWORD_DEFAULT = "admin"

        val LOCAL_PROPS = listOf("httpUrl", "type", "password", "jvmOpts", "startOpts", "runModes", "debugPort")

        val REMOTE_PROPS = listOf("httpUrl", "type", "user", "password")

        fun parse(aem: AemExtension, str: String, configurer: RemoteInstance.() -> Unit = {}): List<RemoteInstance> {
            return (Formats.toList(str) ?: listOf()).map { RemoteInstance.create(aem, it, configurer) }
        }

        @Suppress("ComplexMethod")
        fun properties(aem: AemExtension): List<Instance> {
            return aem.project.properties.filterKeys {
                Patterns.wildcard(it, "instance.*.httpUrl")
            }.keys.mapNotNull { property ->
                val name = property.split(".")[1]
                val nameParts = name.split("-")
                if (nameParts.size != 2) {
                    aem.logger.warn("Instance name has invalid format '$name' in property '$property'.")
                    return@mapNotNull null
                }

                val props = aem.project.properties.filterKeys {
                    Patterns.wildcard(it, "instance.$name.*")
                }.entries.fold(mutableMapOf<String, String>()) { result, e ->
                    val (key, value) = e
                    val prop = key.substringAfter("instance.$name.")
                    result.apply { put(prop, value as String) }
                }

                if (props["httpUrl"].isNullOrBlank()) {
                    aem.logger.warn("Instance named '$name' must have property 'httpUrl' defined.")
                    return@mapNotNull null
                }

                val httpUrl = props["httpUrl"]!!
                val type = PhysicalType.of(props["type"]) ?: PhysicalType.REMOTE
                val (environment, id) = nameParts

                when (type) {
                    PhysicalType.LOCAL -> LocalInstance.create(aem, httpUrl) {
                        this.environment = environment
                        this.id = id

                        props["password"]?.let { this.password = it }
                        props["jvmOpts"]?.let { this.jvmOpts = it.split(" ") }
                        props["startOpts"]?.let { this.startOpts = it.split(" ") }
                        props["runModes"]?.let { this.runModes = it.split(",") }
                        props["debugPort"]?.let { this.debugPort = it.toInt() }

                        this.properties.putAll(props.filterKeys { !LOCAL_PROPS.contains(it) })
                    }
                    PhysicalType.REMOTE -> RemoteInstance.create(aem, httpUrl) {
                        this.environment = environment
                        this.id = id

                        props["user"]?.let { this.user = it }
                        props["password"]?.let { this.password = it }

                        this.properties.putAll(props.filterKeys { !REMOTE_PROPS.contains(it) })
                    }
                }
            }.sortedBy { it.name }
        }

        fun defaults(aem: AemExtension, configurer: RemoteInstance.() -> Unit = {}): List<RemoteInstance> {
            return listOf(
                    RemoteInstance.create(aem, URL_AUTHOR_DEFAULT, configurer),
                    RemoteInstance.create(aem, URL_PUBLISH_DEFAULT, configurer)
            )
        }
    }
}

val Collection<Instance>.names: String
    get() = if (isNotEmpty()) joinToString(", ") { it.name } else "none"