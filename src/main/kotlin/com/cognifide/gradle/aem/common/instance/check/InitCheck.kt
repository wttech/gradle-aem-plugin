package com.cognifide.gradle.aem.common.instance.check

class InitCheck(group: CheckGroup) : DefaultCheck(group) {

    override fun check() = instance.whenLocal {
        if (!initialized) {
            logger.info("Checking auth on $instance")

            // TODO write lock when authorized, do not block other checks but force instanhttpclient to use other creds basing on lock
            val authorizable = state(sync.status.checkAuthorizable())
            if (!authorizable) {
                statusLogger.error(
                    "Auth not ready",
                    "Cannot authorize on $instance"
                )
            }
        }
    }
}
