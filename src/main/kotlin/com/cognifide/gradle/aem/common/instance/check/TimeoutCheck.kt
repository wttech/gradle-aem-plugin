package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.common.utils.Formats
import java.util.concurrent.TimeUnit

class TimeoutCheck(group: CheckGroup) : DefaultCheck(group) {

    var constantTimeout: Long = TimeUnit.MINUTES.toMillis(30)

    var stateTimeout: Long = TimeUnit.MINUTES.toMillis(1)

    override fun check() {
        if (runner.stateTime > stateTimeout) {
            throw InstanceException("Instance state timeout reached '${Formats.duration(stateTimeout)}' for $instance!")
        }

        if (runner.runningTime > constantTimeout) {
            throw InstanceException("Instance constant timeout reached '${Formats.duration(constantTimeout)}'!")
        }
    }
}