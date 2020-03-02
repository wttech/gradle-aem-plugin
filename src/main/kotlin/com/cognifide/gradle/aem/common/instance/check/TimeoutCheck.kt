package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.common.utils.Formats
import java.util.concurrent.TimeUnit

class TimeoutCheck(group: CheckGroup) : DefaultCheck(group) {

    /**
     * Prevents too long inactivity time.
     */
    val stateTime = aem.obj.long { convention(TimeUnit.MINUTES.toMillis(10)) }

    /**
     * Prevents circular restarting of OSGi bundles & components.
     */
    val constantTime = aem.obj.long { convention(TimeUnit.MINUTES.toMillis(30)) }

    override fun check() {
        if (progress.stateTime >= stateTime.get()) {
            throw InstanceException("Instance state timeout reached '${Formats.duration(progress.stateTime)}' for $instance!")
        }

        if (runner.runningTime >= constantTime.get()) {
            throw InstanceException("Instance constant timeout reached '${Formats.duration(runner.runningTime)}'!")
        }
    }
}
