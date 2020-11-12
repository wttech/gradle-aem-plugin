package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension

/**
 * System service related options.
 */
class ServiceOptions(private val aem: AemExtension) {

    /**
     * System user used to run instance.
     */
    val user = aem.obj.string {
        convention("aem")
        aem.prop.string("localInstance.service.user")?.let { set(it) }
    }

    /**
     * System group of user used to run instance.
     */
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

    /**
     * Environment variables loader command.
     */
    val environmentCommand = aem.obj.string {
        convention(". /etc/profile")
        aem.prop.string("localInstance.service.environmentCommand")?.let { set(it) }
    }

    /**
     * Build command relative to root project used to start process which runs AEM instance as a system service.
     */
    val startCommand = aem.obj.string {
        convention("sh gradlew -i --console=plain instanceUp")
        aem.prop.string("localInstance.service.startCommand")?.let { set(it) }
    }

    /**
     * Build command relative to root project used to stop process which runs AEM instance as a system service.
     */
    val stopCommand = aem.obj.string {
        convention("sh gradlew -i --console=plain instanceDown")
        aem.prop.string("localInstance.service.stopCommand")?.let { set(it) }
    }

    /**
     * Build command relative to root project used to describe status of  AEM instance running as a system service.
     */
    val statusCommand = aem.obj.string {
        convention("sh gradlew -q --console=plain instanceStatus")
        aem.prop.string("localInstance.service.statusCommand")?.let { set(it) }
    }

    /**
     * Effective values (shorthand)
     */
    val opts get() = mapOf(
            "user" to user.orNull,
            "group" to group.orNull,
            "limitNoFile" to limitNoFile.orNull,
            "environmentCommand" to environmentCommand.orNull,
            "startCommand" to startCommand.orNull,
            "stopCommand" to stopCommand.orNull,
            "statusCommand" to statusCommand.orNull
    )
}
