package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemVersion
import com.cognifide.gradle.aem.common.instance.action.AwaitDownAction
import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import com.cognifide.gradle.aem.common.instance.action.CheckAction
import com.cognifide.gradle.aem.common.instance.action.ReloadAction
import com.cognifide.gradle.aem.common.instance.check.CheckRunner
import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.common.utils.Patterns
import com.cognifide.gradle.common.utils.formats.JsonPassword
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import java.io.Serializable
import java.time.ZoneId
import java.time.LocalDateTime
import java.time.ZoneOffset

open class Instance(@Transient @get:JsonIgnore protected val aem: AemExtension) : Serializable {

    @Transient
    @JsonIgnore
    protected val common = aem.common

    @Transient
    @JsonIgnore
    protected val logger = aem.logger

    lateinit var httpUrl: String

    @get:JsonIgnore
    val httpPort: Int get() = InstanceUrl.parse(httpUrl).httpPort

    @get:JsonIgnore
    val httpBasicAuthUrl: String get() = InstanceUrl.parse(httpUrl).basicAuth(user, password)

    open lateinit var user: String

    @get:JsonSerialize(using = JsonPassword::class, `as` = String::class)
    lateinit var password: String

    @get:JsonIgnore
    val credentials: Pair<String, String> get() = user to password

    @get:JsonIgnore
    val credentialsString get() = "$user:$password"

    @get:JsonIgnore
    val hiddenPassword: String get() = "*".repeat(password.length)

    lateinit var env: String

    @get:JsonIgnore
    val cmd: Boolean get() = env == ENV_CMD

    lateinit var id: String

    val type: IdType get() = IdType.byId(id)

    val physicalType: PhysicalType get() = PhysicalType.byInstance(this)

    @get:JsonIgnore
    var enabled: Boolean = true

    @get:JsonIgnore
    val local: Boolean get() = physicalType == PhysicalType.LOCAL

    fun <T> local(action: LocalInstance.() -> T) = when (this) {
        is LocalInstance -> this.run(action)
        else -> throw InstanceException("Instance '$name' is not defined as local!")
    }

    fun <T> whenLocal(action: LocalInstance.() -> T) {
        if (local) {
            local(action)
        }
    }

    @get:JsonIgnore
    val author: Boolean get() = type == IdType.AUTHOR

    @get:JsonIgnore
    val publish: Boolean get() = type == IdType.PUBLISH

    var name: String
        get() = "$env-$id"
        set(value) {
            env = value.substringBefore("-")
            id = value.substringAfter("-")
        }

    @get:JsonIgnore
    val sync get() = InstanceSync(aem, this)

    var properties = mutableMapOf<String, String?>()

    @get:JsonIgnore
    val systemProperties: Map<String, String> get() = sync.status.systemProperties

    @get:JsonIgnore
    val slingProperties: Map<String, String> get() = sync.status.slingProperties

    @get:JsonIgnore
    val slingSettings: Map<String, String> get() = sync.status.slingSettings

    fun property(key: String, value: String?) {
        properties[key] = value
    }

    fun property(key: String): String? = properties[key]
            ?: systemProperties[key]
            ?: slingProperties[key]
            ?: slingSettings[key]

    @get:JsonIgnore
    val available: Boolean get() = sync.status.available

    fun checkAvailable(): Boolean = sync.status.checkAvailable()

    @get:JsonIgnore
    val zoneId: ZoneId get() = systemProperties["user.timezone"]?.let { ZoneId.of(it) }
            ?: throw InstanceException("Cannot read timezone of $this!")

    @get:JsonIgnore
    val zoneOffset: ZoneOffset get() = zoneId.rules.getOffset(LocalDateTime.now())

    @get:JsonIgnore
    val zoneInfo: String get() = "${zoneId.id} (GMT${zoneOffset.id})"

    fun date(timestamp: Long) = Formats.dateAt(timestamp, zoneId)

    @get:JsonIgnore
    val osInfo: String get() = mutableListOf<String>().apply {
        systemProperties["os.name"]?.let { add(it) }
        systemProperties["os.arch"]?.let { add(it) }
        systemProperties["os.version"]?.let { add("($it)") }
    }.joinToString(" ")

    @get:JsonIgnore
    val javaInfo: String get() = mutableListOf<String>().apply {
        systemProperties["java.vm.name"]?.let { add(it.removePrefix("Java ")) }
        systemProperties["java.version"]?.let { add("($it)") }
    }.joinToString(" ")

    @get:JsonIgnore
    val runningPath: String get() = systemProperties["user.dir"]
            ?: throw InstanceException("Cannot read running path of $this!")

    @get:JsonIgnore
    val runningModes: List<String> get() = slingSettings["Run_Modes"]
            ?.removeSurrounding("[", "]")
            ?.split(",")?.map { it.trim() }
            ?: throw InstanceException("Cannot read running modes of $this!")

    @get:JsonIgnore
    open val version: AemVersion get() = AemVersion(sync.status.productVersion)

    @get:JsonIgnore
    val manager: InstanceManager get() = aem.instanceManager

    fun awaitUp(options: AwaitUpAction.() -> Unit = {}) = manager.awaitUp(this, options)

    fun awaitDown(options: AwaitDownAction.() -> Unit = {}) = manager.awaitDown(this, options)

    fun awaitReloaded(reloadOptions: ReloadAction.() -> Unit = {}, awaitUpOptions: AwaitUpAction.() -> Unit = {}) {
        manager.awaitReloaded(this, reloadOptions, awaitUpOptions)
    }

    fun reload(options: ReloadAction.() -> Unit = {}) = manager.reload(this, options)

    fun check(options: CheckAction.() -> Unit) = manager.check(this, options)

    fun checkState(options: CheckRunner.() -> Unit = {}) = CheckRunner(aem).apply {
        checks { listOf(bundles(), events(), components()) }
        options()
    }.check(this)

    @get:JsonIgnore
    val state: String get() = checkState().summary

    fun provision() = manager.provisioner.provision(this)

    fun tail() = manager.tailer.tail(this)

    fun reportStatus() = manager.statusReporter.report(this)

    fun examine() = manager.examine(this)

    fun <T> sync(action: InstanceSync.() -> T): T = sync.run(action)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Instance

        return EqualsBuilder()
                .append(name, other.name)
                .append(httpUrl, other.httpUrl)
                .isEquals
    }

    override fun hashCode(): Int {
        return HashCodeBuilder()
                .append(name)
                .append(httpUrl)
                .toHashCode()
    }

    override fun toString(): String = "Instance(name='$name', httpUrl='$httpUrl')"

    @get:JsonIgnore
    val json get() = Formats.toJson(this)

    @Suppress("ThrowsCount")
    fun validate() {
        if (httpUrl.isBlank()) {
            throw AemException("HTTP URL cannot be blank in $this")
        }

        if (user.isBlank()) {
            throw AemException("User cannot be blank in $this")
        }

        if (password.isBlank()) {
            throw AemException("Password cannot be blank in $this")
        }

        if (env.isBlank()) {
            throw AemException("Environment cannot be blank in $this")
        }

        if (id.isBlank()) {
            throw AemException("ID cannot be blank in $this")
        }
    }

    companion object {

        fun create(aem: AemExtension, httpUrl: String, configurer: Instance.() -> Unit = {}): Instance {
            return Instance(aem).apply {
                val instanceUrl = InstanceUrl.parse(httpUrl)

                this.httpUrl = instanceUrl.httpUrl
                this.user = instanceUrl.user
                this.password = instanceUrl.password
                this.env = instanceUrl.env
                this.id = instanceUrl.id

                configurer()
                validate()
            }
        }

        const val FILTER_ANY = "*"

        const val ENV_CMD = "cmd"

        const val URL_AUTHOR_DEFAULT = "http://localhost:4502"

        const val URL_PUBLISH_DEFAULT = "http://localhost:4503"

        const val USER_DEFAULT = "admin"

        const val PASSWORD_DEFAULT = "admin"

        val LOCAL_PROPS = listOf("httpUrl", "enabled", "type", "password", "jvmOpts", "startOpts", "runModes",
                "debugPort", "debugAddress", "openPath")

        val REMOTE_PROPS = listOf("httpUrl", "enabled", "type", "user", "password")

        fun defaultPair(aem: AemExtension) = listOf(defaultAuthor(aem), defaultPublish(aem))

        fun defaultAuthor(aem: AemExtension) = create(aem, URL_AUTHOR_DEFAULT)

        fun defaultPublish(aem: AemExtension) = create(aem, URL_PUBLISH_DEFAULT)

        fun parse(aem: AemExtension, str: String, configurer: Instance.() -> Unit = {}): List<Instance> {
            return (Formats.toList(str) ?: listOf()).map { create(aem, it, configurer) }
        }

        @Suppress("ComplexMethod")
        fun properties(aem: AemExtension): List<Instance> {
            val allProps = aem.project.rootProject.properties
            return allProps.filterKeys {
                Patterns.wildcard(it, "instance.*.httpUrl")
            }.keys.mapNotNull { property ->
                val name = property.split(".")[1]
                val nameParts = name.split("-")
                if (nameParts.size != 2) {
                    aem.logger.warn("Instance name has invalid format '$name' in property '$property'.")
                    return@mapNotNull null
                }

                val props = allProps.filterKeys {
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
                val (env, id) = nameParts

                when (type) {
                    PhysicalType.LOCAL -> LocalInstance.create(aem, httpUrl) {
                        this.env = env
                        this.id = id

                        props["enabled"]?.let { this.enabled = it.toBoolean() }
                        props["password"]?.let { this.password = it }
                        props["jvmOpts"]?.let { this.jvmOpts = it.split(" ") }
                        props["startOpts"]?.let { this.startOpts = it.split(" ") }
                        props["runModes"]?.let { this.runModes = it.split(",") }
                        props["debugPort"]?.let { this.debugPort = it.toInt() }
                        props["debugAddress"]?.let { this.debugAddress = it }
                        props["openPath"]?.let { this.openPath = it }

                        this.properties.putAll(props.filterKeys { !LOCAL_PROPS.contains(it) })
                    }
                    PhysicalType.REMOTE -> create(aem, httpUrl) {
                        this.env = env
                        this.id = id

                        props["enabled"]?.let { this.enabled = it.toBoolean() }
                        props["user"]?.let { this.user = it }
                        props["password"]?.let { this.password = it }

                        this.properties.putAll(props.filterKeys { !REMOTE_PROPS.contains(it) })
                    }
                }
            }.sortedBy { it.name }
        }
    }
}

val Collection<Instance>.names: String
    get() = if (isNotEmpty()) joinToString(", ") { it.name } else "none"
