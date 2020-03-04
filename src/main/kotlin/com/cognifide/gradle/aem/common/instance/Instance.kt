package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemExtension
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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

open class Instance(@Transient @JsonIgnore protected val aem: AemExtension) : Serializable {

    protected val common = aem.common

    protected val logger = aem.logger

    @get:Input
    lateinit var httpUrl: String

    @get:Internal
    @get:JsonIgnore
    val httpPort: Int get() = InstanceUrl.parse(httpUrl).httpPort

    @get:Internal
    @get:JsonIgnore
    val httpBasicAuthUrl: String get() = InstanceUrl.parse(httpUrl).basicAuth(user, password)

    @get:Input
    open lateinit var user: String

    @get:Input
    @get:JsonSerialize(using = JsonPassword::class, `as` = String::class)
    lateinit var password: String

    @get:Internal
    @get:JsonIgnore
    val credentials: Pair<String, String> get() = user to password

    @get:Internal
    @get:JsonIgnore
    val credentialsString get() = "$user:$password"

    @get:Internal
    @get:JsonIgnore
    val hiddenPassword: String get() = "*".repeat(password.length)

    @get:Input
    lateinit var environment: String

    @get:Internal
    @get:JsonIgnore
    val cmd: Boolean get() = environment == ENVIRONMENT_CMD

    @get:Input
    lateinit var id: String

    @get:Internal
    val type: IdType get() = IdType.byId(id)

    @get:Internal
    @get:JsonIgnore
    val author: Boolean get() = type == IdType.AUTHOR

    @get:Internal
    @get:JsonIgnore
    val publish: Boolean get() = type == IdType.PUBLISH

    @get:Internal
    var name: String
        get() = "$environment-$id"
        set(value) {
            environment = value.substringBefore("-")
            id = value.substringAfter("-")
        }

    @get:Internal
    @get:JsonIgnore
    val sync get() = InstanceSync(aem, this)

    @get:Input
    var properties = mutableMapOf<String, String?>()

    @get:JsonIgnore
    val systemProperties: Map<String, String> get() = sync.status.systemProperties

    fun property(key: String, value: String?) {
        properties[key] = value
    }

    fun property(key: String): String? = properties[key] ?: systemProperties[key]

    @get:Internal
    @get:JsonIgnore
    val available: Boolean get() = systemProperties.isNotEmpty()

    @get:Internal
    @get:JsonIgnore
    val zoneId: ZoneId get() = property("user.timezone")?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()

    @get:JsonIgnore
    open val version: String get() = sync.status.productVersion

    /**
     * Indicates repository restructure performed in AEM 6.4.0 / preparations for making AEM available on cloud.
     *
     * After this changes, nodes under '/apps' or '/libs' are frozen and some features (like workflow manager)
     * requires to copy these nodes under '/var' by plugin (or AEM itself).
     *
     * @see <https://docs.adobe.com/content/help/en/experience-manager-64/deploying/restructuring/repository-restructuring.html>
     */
    val frozen get() = Formats.versionAtLeast(version, "6.4.0")

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

    fun satisfy() = manager.satisfier.satisfy(this)

    fun tail() = manager.tailer.tail(this)

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

    @get:Internal
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

        if (environment.isBlank()) {
            throw AemException("Environment cannot be blank in $this")
        }

        if (id.isBlank()) {
            throw AemException("Type name cannot be blank in $this")
        }
    }

    companion object {

        fun create(aem: AemExtension, httpUrl: String, configurer: Instance.() -> Unit = {}): Instance {
            return Instance(aem).apply {
                val instanceUrl = InstanceUrl.parse(httpUrl)

                this.httpUrl = instanceUrl.httpUrl
                this.user = instanceUrl.user
                this.password = instanceUrl.password
                this.environment = aem.commonOptions.env.get()
                this.id = instanceUrl.id

                configurer()
                validate()
            }
        }

        const val FILTER_ANY = "*"

        const val ENVIRONMENT_CMD = "cmd"

        const val URL_AUTHOR_DEFAULT = "http://localhost:4502"

        const val URL_PUBLISH_DEFAULT = "http://localhost:4503"

        const val USER_DEFAULT = "admin"

        const val PASSWORD_DEFAULT = "admin"

        val LOCAL_PROPS = listOf("httpUrl", "type", "password", "jvmOpts", "startOpts", "runModes", "debugPort", "debugAddress")

        val REMOTE_PROPS = listOf("httpUrl", "type", "user", "password")

        fun defaultPair(aem: AemExtension) = listOf(defaultAuthor(aem), defaultPublish(aem))

        fun defaultAuthor(aem: AemExtension) = create(aem, URL_AUTHOR_DEFAULT)

        fun defaultPublish(aem: AemExtension) = create(aem, URL_PUBLISH_DEFAULT)

        fun parse(aem: AemExtension, str: String, configurer: Instance.() -> Unit = {}): List<Instance> {
            return (Formats.toList(str) ?: listOf()).map { create(aem, it, configurer) }
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
                        props["debugAddress"]?.let { this.debugAddress = it }

                        this.properties.putAll(props.filterKeys { !LOCAL_PROPS.contains(it) })
                    }
                    PhysicalType.REMOTE -> create(aem, httpUrl) {
                        this.environment = environment
                        this.id = id

                        props["user"]?.let { this.user = it }
                        props["password"]?.let { this.password = it }

                        this.properties.putAll(props.filterKeys { !REMOTE_PROPS.contains(it) })
                    }
                }
            }.sortedBy { it.name }
        }

        fun defaults(aem: AemExtension, configurer: Instance.() -> Unit = {}) = listOf(
                create(aem, URL_AUTHOR_DEFAULT, configurer),
                create(aem, URL_PUBLISH_DEFAULT, configurer)
        )
    }
}

val Collection<Instance>.names: String
    get() = if (isNotEmpty()) joinToString(", ") { it.name } else "none"

fun Collection<Instance>.check() {
    checkAvailable()
    checkRunningOther()
}

fun Collection<Instance>.checkAvailable() {
    val unavailable = filter { !it.available }
    if (unavailable.isNotEmpty()) {
        throw InstanceException("Instances are unavailable (${unavailable.size}):\n" +
                unavailable.joinToString("\n") { "Instance '${it.name}' at URL '${it.httpUrl}'" } + "\n\n" +
                "Ensure having correct URLs defined, credentials correctly encoded and networking in correct state (internet accessible, VPN on/off)"
        )
    }
}

fun Collection<Instance>.checkRunningOther() {
    val running = filterIsInstance<LocalInstance>().filter { it.runningOther }
    if (running.isNotEmpty()) {
        throw InstanceException("Instances are already running (${running.size}):\n" +
                running.joinToString("\n") { "Instance '${it.name}' at URL '${it.httpUrl}' located at path '${it.runningDir}'" } + "\n\n" +
                "Ensure having these instances down."
        )
    }
}
