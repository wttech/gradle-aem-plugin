package com.cognifide.gradle.aem.common.instance.service.repository

import com.cognifide.gradle.aem.common.instance.Instance

class ReplicationAgent(val page: Node) {

    val name get() = page.name

    val location get() = page.parent.name.substringAfter("agents.")

    val pageContent get() = page.child("jcr:content")

    val enabled get() = pageContent.properties.boolean("enabled")

    fun toggle(flag: Boolean) = pageContent.saveProperty("enabled", flag)

    fun enable() = toggle(true)

    fun disable() = toggle(false)

    fun enable(transportUri: String, props: Map<String, Any?> = mapOf()) = configure(mapOf(
            "enabled" to true,
            "transportUri" to transportUri
    ) + props)

    fun enable(instance: Instance, props: Map<String, Any?> = mapOf()) = configure(mapOf(
            "enabled" to true,
            "transportUri" to "${instance.httpUrl}/bin/receive?sling:authRequestLogin=1",
            "transportUser" to instance.user,
            "transportPassword" to instance.password,
            "userId" to instance.user
    ) + props)

    fun configure(propName: String, propValue: Any?) = configure(mapOf(propName to propValue))

    fun configure(props: Map<String, Any?>) {
        if (!page.exists) page.save(mapOf(
                "jcr:primaryType" to "cq:Page"
        ))
        pageContent.save(if (!pageContent.exists) mapOf(
                "jcr:primaryType" to "nt:unstructured",
                "jcr:title" to name.capitalize(),
                "sling:resourceType" to "cq/replication/components/agent",
                "cq:template" to "/libs/cq/replication/templates/agent"
        ) + props else props)
    }

    fun delete() = page.delete()

    override fun toString() = "ReplicationAgent(location=$location, name=$name)"

    companion object {
        const val LOCATION_AUTHOR = "author"

        const val LOCATION_PUBLISH = "publish"
    }
}
