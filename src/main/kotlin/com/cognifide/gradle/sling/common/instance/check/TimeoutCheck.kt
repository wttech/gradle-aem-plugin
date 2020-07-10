package com.cognifide.gradle.sling.common.instance.check

import com.cognifide.gradle.sling.common.instance.InstanceException
import com.cognifide.gradle.common.utils.Formats
import java.util.concurrent.TimeUnit

class TimeoutCheck(group: CheckGroup) : DefaultCheck(group) {

    /**
     * Prevents too long unavailability time (instance never responds anything).
     */
    val unavailableTime = sling.obj.long { convention(TimeUnit.MINUTES.toMillis(1)) }

    /**
     * Prevents too long inactivity time (instance state is no longer changing).
     */
    val stateTime = sling.obj.long { convention(TimeUnit.MINUTES.toMillis(10)) }

    /**
     * Prevents circular restarting of OSGi bundles & components (e.g installing SP/CFP takes too much time).
     */
    val constantTime = sling.obj.long { convention(TimeUnit.MINUTES.toMillis(30)) }

    override fun check() {
        if (!instance.available && progress.stateTime >= unavailableTime.get()) {
            throw InstanceException("Instance unavailable timeout reached '${Formats.duration(progress.stateTime)}' for $instance!")
        }

        if (progress.stateTime >= stateTime.get()) {
            throw InstanceException("Instance state timeout reached '${Formats.duration(progress.stateTime)}' for $instance!")
        }

        if (runner.runningTime >= constantTime.get()) {
            throw InstanceException("Instance constant timeout reached '${Formats.duration(runner.runningTime)}'!")
        }
    }
}
