package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.CommonOptions
import com.cognifide.gradle.common.build.PropertyGroup

class InstanceFactory(val aem: AemExtension) {

    fun defaultPair() = listOf(defaultAuthor(), defaultPublish())

    fun defaultAuthor() = remoteByUrl(InstanceUrl.HTTP_AUTHOR_DEFAULT)

    fun defaultPublish() = remoteByUrl(InstanceUrl.HTTP_PUBLISH_DEFAULT)

    fun create(name: String, options: Instance.() -> Unit = {}) = when (name.substringBefore("-")) {
        CommonOptions.ENVIRONMENT_LOCAL -> local(name)
        else -> remote(name)
    }.apply(options)

    fun createByUrl(httpUrl: String, options: Instance.() -> Unit = {}) = create(InstanceUrl.parse(httpUrl).name, options)

    fun remote(name: String, options: Instance.() -> Unit = {}) = Instance(aem, name).apply {
        options()
        validate()
    }

    fun remoteByUrl(httpUrl: String, options: Instance.() -> Unit = {}) = remote(InstanceUrl.parse(httpUrl).name, options)


    fun local(name: String, options: LocalInstance.() -> Unit = {}) = LocalInstance(aem, name).apply {
        options()
        validate()
    }

    fun localByUrl(httpUrl: String, options: LocalInstance.() -> Unit = {}) = local(InstanceUrl.parse(httpUrl).name, options)

    fun parseProperties() = parseProperties(aem.project.rootProject.properties)

    fun parseProperties(allProps: Map<String, *>) = allProps.filterKeys { prop ->
        !prop.startsWith("$NAME_DEFAULT.") && (
            ALL_PROPS.any {
                Regex("^$PROP_GROUP.$NAME_REGEX.$it\$").matches(prop)
            }
            )
    }.keys.mapNotNull { prop ->
        val name = prop.split(".")[1]
        val nameParts = name.split("-")
        if (nameParts.size != 2) {
            aem.logger.warn("Instance name has invalid format '$name' in property '$prop'.")
            return@mapNotNull null
        }
        name
    }.distinct().sorted().map { create(it) }

    companion object {

        const val PROP_GROUP = "instance"

        const val NAME_DEFAULT = "$PROP_GROUP.${PropertyGroup.DEFAULT}"

        const val NAME_REGEX = "[\\w_]+-[\\w_]+"

        val LOCAL_PROPS = listOf(
            "httpUrl", "enabled", "type", "password", "jvmOpts", "startOpts", "runModes",
            "debugPort", "debugAddress", "openPath"
        )

        val REMOTE_PROPS = listOf("httpUrl", "enabled", "type", "user", "password", "serviceCredentialsUrl")

        val ALL_PROPS = (LOCAL_PROPS + REMOTE_PROPS).toSet()
    }
}
