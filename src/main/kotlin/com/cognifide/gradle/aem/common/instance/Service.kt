package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension

/**
 * System service related options.
 */
class Service(private val aem: AemExtension) {

    val user = aem.obj.string {
        convention("aem")
        aem.prop.string("localInstance.service.user")?.let { set(it) }
    }

    val group = aem.obj.string {
        convention("aem")
        aem.prop.string("localInstance.service.group")?.let { set(it) }
    }

    /**
     * Controls number of file descriptors allowed.
     */
    val limitNoFile = aem.obj.int {
        convention(20000)
        aem.prop.int("localInstance.service.limitNoFile")?.let { set(it) }
    }

    val opts get() = mapOf(
            "user" to user.orNull,
            "group" to group.orNull,
            "limitNoFile" to limitNoFile.orNull
    )
}