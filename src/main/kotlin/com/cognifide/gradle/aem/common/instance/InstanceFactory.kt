package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.common.utils.Patterns

class InstanceFactory(val aem: AemExtension) {

    private val bearerToken by lazy { aem.instanceManager.ims.generateToken() }

    fun defaultPair() = listOf(defaultAuthor(), defaultPublish())

    fun defaultAuthor() = remote(InstanceUrl.HTTP_AUTHOR_DEFAULT)

    fun defaultPublish() = remote(InstanceUrl.HTTP_PUBLISH_DEFAULT)

    fun remote(httpUrl: String, configurer: Instance.() -> Unit = {}): Instance {
        return Instance(aem).apply {
            val instanceUrl = InstanceUrl.parse(httpUrl)

            this.httpUrl.set(instanceUrl.httpUrl)
            this.user.set(instanceUrl.user)
            this.password.set(instanceUrl.password)
            this.bearerToken.set(aem.obj.provider { this@InstanceFactory.bearerToken })
            this.env.set(instanceUrl.env)
            this.id.set(instanceUrl.id)

            configurer()
            validate()
        }
    }

    fun local(httpUrl: String, configurer: LocalInstance.() -> Unit = {}): LocalInstance {
        return LocalInstance(aem).apply {
            val instanceUrl = InstanceUrl.parse(httpUrl)
            if (instanceUrl.user != LocalInstance.USER) {
                throw LocalInstanceException("User '${instanceUrl.user}' (other than 'admin') is not allowed while using local instance(s).")
            }

            this.httpUrl.set(instanceUrl.httpUrl)
            this.password.set(instanceUrl.password)
            this.id.set(instanceUrl.id)
            this.debugPort.set(instanceUrl.debugPort)
            this.env.set(instanceUrl.env)

            configurer()
            validate()
        }
    }

    fun parse(str: String, configurer: Instance.() -> Unit = {}): List<Instance> {
        return (Formats.toList(str) ?: listOf()).map { remote(it, configurer) }
    }

    fun parseProperties() = parseProperties(aem.project.rootProject.properties)

    fun parseProperties(allProps: Map<String, *>): List<Instance> {
        val instanceNames = allProps.filterKeys { prop ->
            !prop.startsWith("$NAME_DEFAULT.") && (
                ALL_PROPS.any {
                    Regex("^instance.$NAME_REGEX.$it\$").matches(prop)
                }
                )
        }.keys.mapNotNull { p ->
            val name = p.split(".")[1]
            val nameParts = name.split("-")
            if (nameParts.size != 2) {
                aem.logger.warn("Instance name has invalid format '$name' in property '$p'.")
                return@mapNotNull null
            }
            name
        }.distinct()

        return instanceNames.sorted().fold(mutableListOf()) { result, name ->
            val defaultProps = prefixedProperties(allProps, NAME_DEFAULT)
            val props = defaultProps + prefixedProperties(allProps, "instance.$name")
            result.add(singleFromProperties(name, props, result))
            result
        }
    }

    private fun prefixedProperties(allProps: Map<String, *>, prefix: String) = allProps.filterKeys {
        Patterns.wildcard(it, "$prefix.*")
    }.entries.fold(mutableMapOf<String, String>()) { result, e ->
        val (key, value) = e
        val prop = key.substringAfter("$prefix.")
        result.apply { put(prop, value as String) }
    }

    private fun singleFromProperties(name: String, props: Map<String, String>, others: List<Instance>): Instance {
        return when (props["type"]?.let { PhysicalType.of(it) } ?: PhysicalType.REMOTE) {
            PhysicalType.LOCAL -> localFromProperties(name, props, others)
            PhysicalType.REMOTE -> remoteFromProperties(name, props, others)
        }
    }

    private fun localFromProperties(name: String, props: Map<String, String>, others: List<Instance>): LocalInstance {
        val httpUrl = props["httpUrl"] ?: httpUrlProperty(name, others)
        return local(httpUrl) {
            this.name = name
            props["enabled"]?.let { this.enabled.set(it.toBoolean()) }
            props["password"]?.let { this.password.set(it) }
            props["jvmOpts"]?.let { this.jvmOpts.set(it.split(" ")) }
            props["startOpts"]?.let { this.startOpts.set(it.split(" ")) }
            props["runModes"]?.let { this.runModes.set(it.split(",")) }
            props["debugPort"]?.let { this.debugPort.set(it.toInt()) }
            props["debugAddress"]?.let { this.debugAddress.set(it) }
            props["openPath"]?.let { this.openPath.set(it) }
            this.properties.putAll(props.filterKeys { !LOCAL_PROPS.contains(it) })
        }
    }

    private fun remoteFromProperties(name: String, props: Map<String, String>, others: List<Instance>): Instance {
        val httpUrl = props["httpUrl"] ?: httpUrlProperty(name, others)
        return remote(httpUrl) {
            this.name = name
            props["enabled"]?.let { this.enabled.set(it.toBoolean()) }
            props["user"]?.let { this.user.set(it) }
            props["password"]?.let { this.password.set(it) }
            this.properties.putAll(props.filterKeys { !REMOTE_PROPS.contains(it) })
        }
    }

    private fun httpUrlProperty(name: String, others: List<Instance>): String {
        val type = IdType.byId(name.split("-")[1])
        val port = others.filter { it.type == type }.maxOfOrNull { it.httpPort }?.let { it + 1 } ?: type.httpPortDefault
        return "${InstanceUrl.HTTP_HOST_DEFAULT}:$port"
    }

    companion object {
        const val NAME_DEFAULT = "instance.default"

        const val NAME_REGEX = "[\\w_]+-[\\w_]+"

        val LOCAL_PROPS = listOf(
            "httpUrl", "enabled", "type", "password", "jvmOpts", "startOpts", "runModes",
            "debugPort", "debugAddress", "openPath"
        )

        val REMOTE_PROPS = listOf("httpUrl", "enabled", "type", "user", "password")

        val ALL_PROPS = (LOCAL_PROPS + REMOTE_PROPS).toSet()
    }
}
