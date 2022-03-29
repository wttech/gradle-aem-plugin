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
import com.cognifide.gradle.common.utils.formats.JsonPassword
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import java.io.Serializable
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

open class Instance(@Transient @get:JsonIgnore protected val aem: AemExtension) : Serializable {

    @Transient
    @JsonIgnore
    protected val common = aem.common

    @Transient
    @JsonIgnore
    protected val logger = aem.logger

    lateinit var httpUrl: String

    private val httpUrlDetails get() = InstanceUrl.parse(httpUrl)

    @get:JsonIgnore
    val httpPort get() = httpUrlDetails.httpPort

    @get:JsonIgnore
    val httpHost get() = httpUrlDetails.httpHost

    @get:JsonIgnore
    val httpBasicAuthUrl: String get() = httpUrlDetails.basicAuth(user, password)

    open lateinit var user: String

    @get:JsonSerialize(using = JsonPassword::class, `as` = String::class)
    lateinit var password: String

    @get:JsonIgnore
    val credentials: Pair<String, String> get() = when (this) {
        is LocalInstance -> auth.credentials
        else -> user to password
    }

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
    val reachable: Boolean get() = sync.status.reachable

    @get:JsonIgnore
    val available: Boolean get() = sync.status.available

    @get:JsonIgnore
    val zoneId: ZoneId get() = systemProperties["user.timezone"]?.let { ZoneId.of(it) }
        ?: throw InstanceException("Cannot read timezone of $this!")

    @get:JsonIgnore
    val zoneOffset: ZoneOffset get() = zoneId.rules.getOffset(LocalDateTime.now())

    @get:JsonIgnore
    val zoneInfo: String get() = "${zoneId.id} (GMT${zoneOffset.id})"

    fun date(timestamp: Long) = try {
        Formats.dateAt(timestamp, zoneId)
    } catch (e: InstanceException) {
        logger.debug("Cannot format instance date, because timezone cannot be read on $this!", e)
        Formats.dateAt(timestamp, ZoneId.systemDefault())
    }

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

        const val FILTER_ANY = "*"

        const val ENV_CMD = "cmd"

        const val USER_DEFAULT = "admin"

        const val PASSWORD_DEFAULT = "admin"

        val CREDENTIALS_DEFAULT = USER_DEFAULT to PASSWORD_DEFAULT
    }
}

val Collection<Instance>.names: String
    get() = if (isNotEmpty()) joinToString(", ") { it.name } else "none"
