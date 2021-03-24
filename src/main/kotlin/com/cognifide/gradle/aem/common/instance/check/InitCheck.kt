package com.cognifide.gradle.aem.common.instance.check

class InitCheck(group: CheckGroup) : DefaultCheck(group) {

    override fun check() = instance.whenLocal {
        if (state(auth.becameAvailable())) {
            statusLogger.error(
                "Auth became available",
                "Switching auth credentials for $instance"
            )
        }
    }
}
