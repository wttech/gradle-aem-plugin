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
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import java.io.Serializable
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

open class Instance(protected val aem: AemExtension) : Serializable {

    protected val common = aem.common

    protected val logger = aem.logger

    val httpUrl = common.obj.string {}

    private val httpUrlDetails get() = InstanceUrl.parse(httpUrl.get())

    val httpPort get() = httpUrlDetails.httpPort

    val httpHost get() = httpUrlDetails.httpHost

    val httpBasicAuthUrl: String get() = httpUrlDetails.basicAuth(user.get(), password.get())

    val user = common.obj.string { }

    val password = common.obj.string {}

    val credentials: Pair<String, String> get() = when (this) {
        is LocalInstance -> auth.credentials
        else -> user.get() to password.get()
    }

    val credentialsString get() = "$user:$password"

    val hiddenPassword: String get() = "*".repeat(password.get().length)

    val env = common.obj.string {}

    val cmd: Boolean get() = env.get() == ENV_CMD

    val id = common.obj.string {}

    val type: IdType get() = IdType.byId(id.get())

    val physicalType: PhysicalType get() = PhysicalType.byInstance(this)

    val enabled = common.obj.boolean { convention(true) }

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

    val author: Boolean get() = type == IdType.AUTHOR

    val publish: Boolean get() = type == IdType.PUBLISH

    var name: String
        get() = "${env.get()}-${id.get()}"
        set(value) {
            env.set(value.substringBefore("-"))
            id.set(value.substringAfter("-"))
        }

    val sync get() = InstanceSync(aem, this)

    val properties = common.obj.map<String, String?> { convention(mapOf()) }

    val systemProperties: Map<String, String> get() = sync.status.systemProperties

    val slingProperties: Map<String, String> get() = sync.status.slingProperties

    val slingSettings: Map<String, String> get() = sync.status.slingSettings

    fun property(key: String): String? = properties.get()[key]
        ?: systemProperties[key]
        ?: slingProperties[key]
        ?: slingSettings[key]

    val reachable: Boolean get() = sync.status.reachable

    val available: Boolean get() = sync.status.available

    val zoneId: ZoneId get() = systemProperties["user.timezone"]?.let { ZoneId.of(it) }
        ?: throw InstanceException("Cannot read timezone of $this!")

    val zoneOffset: ZoneOffset get() = zoneId.rules.getOffset(LocalDateTime.now())

    val zoneInfo: String get() = "${zoneId.id} (GMT${zoneOffset.id})"

    fun date(timestamp: Long) = try {
        Formats.dateAt(timestamp, zoneId)
    } catch (e: InstanceException) {
        logger.debug("Cannot format instance date, because timezone cannot be read on $this!", e)
        Formats.dateAt(timestamp, ZoneId.systemDefault())
    }

    val osInfo: String get() = mutableListOf<String>().apply {
        systemProperties["os.name"]?.let { add(it) }
        systemProperties["os.arch"]?.let { add(it) }
        systemProperties["os.version"]?.let { add("($it)") }
    }.joinToString(" ")

    val javaInfo: String get() = mutableListOf<String>().apply {
        systemProperties["java.vm.name"]?.let { add(it.removePrefix("Java ")) }
        systemProperties["java.version"]?.let { add("($it)") }
    }.joinToString(" ")

    val runningPath: String get() = systemProperties["user.dir"]
        ?: throw InstanceException("Cannot read running path of $this!")

    val runningModes: List<String> get() = slingSettings["Run_Modes"]
        ?.removeSurrounding("[", "]")
        ?.split(",")?.map { it.trim() }
        ?: throw InstanceException("Cannot read running modes of $this!")

    open val version: AemVersion get() = AemVersion(sync.status.productVersion)

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

    override fun toString(): String = "Instance(name='$name', httpUrl='${httpUrl.get()}')"

    @Suppress("ThrowsCount")
    fun validate() {
        if (httpUrl.orNull.isNullOrBlank()) {
            throw AemException("HTTP URL cannot be blank in $this")
        }

        if (user.orNull.isNullOrBlank()) {
            throw AemException("User cannot be blank in $this")
        }

        if (password.orNull.isNullOrBlank()) {
            throw AemException("Password cannot be blank in $this")
        }

        if (env.orNull.isNullOrBlank()) {
            throw AemException("Environment cannot be blank in $this")
        }

        if (id.orNull.isNullOrBlank()) {
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
