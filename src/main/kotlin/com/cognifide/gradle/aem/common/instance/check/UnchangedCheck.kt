package com.cognifide.gradle.aem.common.instance.check

import java.util.concurrent.TimeUnit

/**
 * Check that protects against false-positive CRX package deployments.
 *
 * It awaits instance state to be changed at least one time.
 * If it does not, it just make a little delay to be sure that this state is not temporary.
 */
class UnchangedCheck(group: CheckGroup) : DefaultCheck(group) {

    val awaitTime = aem.obj.long { convention(TimeUnit.SECONDS.toMillis(3)) }

    override fun check() {
        if (progress.stateChanges <= 1 && progress.stateTime < awaitTime.get()) {
            statusLogger.error(
                "State unchanged",
                "Awaiting state to be changed on $instance"
            )
            return
        }
    }
}
