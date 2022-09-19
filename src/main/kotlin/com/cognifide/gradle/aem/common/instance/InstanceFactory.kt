package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.CommonOptions
import com.cognifide.gradle.common.build.PropertyGroup

class InstanceFactory(val aem: AemExtension) {

    fun defaultPair() = listOf(defaultAuthor(), defaultPublish())

    fun defaultAuthor() = remoteByUrl(InstanceUrl.HTTP_AUTHOR_DEFAULT)

    fun defaultPublish() = remoteByUrl(InstanceUrl.HTTP_PUBLISH_DEFAULT)

    fun create(name: String, options: Instance.() -> Unit = {}): Instance {
        val props = PropertyGroup(aem.common.prop, PROP_GROUP, name)
        return when (Location.find(props.string(LOCATION_PROP))) {
            Location.LOCAL -> local(name)
            Location.REMOTE -> remote(name)
            else -> {
                when (name.substringBefore("-")) {
                    CommonOptions.ENVIRONMENT_LOCAL -> local(name)
                    else -> remote(name)
                }
            }
        }.apply(options)
    }

    fun createByUrl(httpUrl: String, options: Instance.() -> Unit = {}) : Instance {
        val parsed = InstanceUrl.parse(httpUrl)
        return create(parsed.name, options).apply { this.httpUrl.set(parsed.httpUrl) }
    }

    fun remote(name: String, options: Instance.() -> Unit = {}) = Instance(aem, name).apply {
        options()
        validate()
    }

    fun remoteByUrl(httpUrl: String, options: Instance.() -> Unit = {}) : Instance {
        val parsed = InstanceUrl.parse(httpUrl)
        return remote(parsed.name, options).apply { this.httpUrl.set(parsed.httpUrl) }
    }

    fun local(name: String, options: LocalInstance.() -> Unit = {}) = LocalInstance(aem, name).apply {
        options()
        validate()
    }

    fun localByUrl(httpUrl: String, options: LocalInstance.() -> Unit = {}) : LocalInstance {
        val parsed = InstanceUrl.parse(httpUrl)
        return local(parsed.name, options).apply { this.httpUrl.set(parsed.httpUrl) }
    }

    fun parseProperties() = parseProperties(aem.project.rootProject.properties)

    fun parseProperties(allProps: Map<String, *>) = allProps.filterKeys { p ->
        !p.startsWith("$NAME_DEFAULT.") && (NAME_PROPS.any { Regex("^$PROP_GROUP.$NAME_REGEX.$it\$").matches(p) })
    }.keys.mapNotNull { prop ->
        val name = prop.split(".")[1]
        val nameParts = name.split("-")
        if (nameParts.size != 2) {
            aem.logger.warn("AEM instance name has invalid format '$name' in property '$prop'.")
            return@mapNotNull null
        }
        name
    }.distinct().sorted().map { create(it) }

    companion object {

        const val PROP_GROUP = "instance"

        const val NAME_DEFAULT = "$PROP_GROUP.${PropertyGroup.DEFAULT}"

        const val NAME_REGEX = "[\\w_]+-[\\w_]+"

        const val LOCATION_PROP = "location"

        const val HTTP_URL_PROP = "httpUrl"

        const val ENABLED_PROP = "enabled"

        val NAME_PROPS = listOf(LOCATION_PROP, HTTP_URL_PROP, ENABLED_PROP).toSet()
    }
}
