package com.cognifide.gradle.aem.common.instance.service.repository

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.utils.filterNotNull

class ReplicationAgent(val page: Node) {

    val instance = page.repository

    val name get() = page.name

    val location get() = page.parent.name.substringAfter("agents.")

    val pageContent get() = page.child("jcr:content")

    val enabled get() = pageContent.properties.boolean("enabled")

    fun toggle(flag: Boolean) = configure("enabled", flag)

    fun enable() = toggle(true)

    fun disable() = toggle(false)

    fun configure(
        enabled: Boolean = true,
        transportUri: String? = null,
        transportUser: String? = null,
        transportPassword: String? = null,
        userId: String? = null
    ) = configure(mapOf(
            "enabled" to enabled,
            "transportUri" to transportUri,
            "transportUser" to transportUser,
            "transportPassword" to transportPassword,
            "userId" to userId
    ).filterNotNull())

    fun configure(instance: Instance, enabled: Boolean = true) = configure(
        enabled, "${instance.httpUrl}/bin/receive?sling:authRequestLogin=1",
        instance.user, instance.password, instance.user
    )

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
