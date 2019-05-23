package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.common.AemException
import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.common.Patterns
import com.cognifide.gradle.aem.common.formats.JsonPassword
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
        get() = InstanceUrl.parse(httpUrl).httpBasicAuthUrl(user, password)

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
    val zoneId: ZoneId

    @get:Input
    val properties: Map<String, Any>

    fun property(key: String, value: Any)

    fun property(key: String): Any?

    fun string(key: String): String?

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

        if (typeName.isBlank()) {
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

        const val TYPE_REMOTE = "remote"

        const val TYPE_LOCAL = "local"

        val LOCAL_PROPS = listOf("httpUrl", "type", "password", "jvmOpts", "startOpts", "runModes", "debugPort", "zoneId")

        val REMOTE_PROPS = listOf("httpUrl", "type", "user", "password", "zoneId")

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

                if (props["httpUrl"] == null) {
                    aem.logger.warn("Instance named '$name' must have property 'httpUrl' defined.")
                    return@mapNotNull null
                }

                val httpUrl = props["httpUrl"]!!
                val type = props["type"] ?: TYPE_REMOTE
                val (environment, typeName) = nameParts

                when (type) {
                    TYPE_LOCAL -> LocalInstance.create(aem, httpUrl) {
                        this.environment = environment
                        this.typeName = typeName

                        props["password"]?.let { this.password = it }
                        props["jvmOpts"]?.let { this.jvmOpts = it.split(" ") }
                        props["startOpts"]?.let { this.startOpts = it.split(" ") }
                        props["runModes"]?.let { this.runModes = it.split(",") }
                        props["debugPort"]?.let { this.debugPort = it.toInt() }
                        props["zoneId"]?.let { this.zoneId = ZoneId.of(it) }

                        this.properties = props.filterKeys { !LOCAL_PROPS.contains(it) }
                    }
                    TYPE_REMOTE -> RemoteInstance.create(aem, httpUrl) {
                        this.environment = environment
                        this.typeName = typeName

                        props["user"]?.let { this.user = it }
                        props["password"]?.let { this.password = it }
                        props["zoneId"]?.let { this.zoneId = ZoneId.of(it) }

                        this.properties = props.filterKeys { !REMOTE_PROPS.contains(it) }
                    }
                    else -> {
                        aem.logger.warn("Invalid instance type '$type' defined in property '$property'. Supported types: 'local', 'remote'.")
                        return@mapNotNull null
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
    get() = joinToString(", ") { it.name }

fun Instance.isInitialized(): Boolean {
    return this !is LocalInstance || initialized
}

fun Instance.isBeingInitialized(): Boolean {
    return this is LocalInstance && !initialized
}