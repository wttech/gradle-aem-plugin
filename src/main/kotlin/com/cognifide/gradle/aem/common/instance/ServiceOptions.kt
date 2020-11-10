package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension

/**
 * System service related options.
 */
class ServiceOptions(private val aem: AemExtension) {

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

    val startCommand = aem.obj.string {
        convention("sh gradlew -i --console=plain instanceUp")
        aem.prop.string("localInstance.service.startCommand")?.let { set(it) }
    }

    val stopCommand = aem.obj.string {
        convention("sh gradlew -i --console=plain instanceDown")
        aem.prop.string("localInstance.service.stopCommand")?.let { set(it) }
    }

    val statusCommand = aem.obj.string {
        convention("sh gradlew -q --console=plain instanceStatus")
        aem.prop.string("localInstance.service.statusCommand")?.let { set(it) }
    }

    val opts get() = mapOf(
            "user" to user.orNull,
            "group" to group.orNull,
            "limitNoFile" to limitNoFile.orNull,
            "startCommand" to startCommand.orNull,
            "stopCommand" to stopCommand.orNull,
            "statusCommand" to statusCommand.orNull
    )
}
