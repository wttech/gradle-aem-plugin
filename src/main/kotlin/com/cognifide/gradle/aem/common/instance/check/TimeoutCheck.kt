package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.common.utils.Formats
import java.util.concurrent.TimeUnit

class TimeoutCheck(group: CheckGroup) : DefaultCheck(group) {

    var state: Long = TimeUnit.MINUTES.toMillis(1)

    var constant: Long = TimeUnit.MINUTES.toMillis(30)

    override fun check() {
        if (stateTime >= state) {
            throw InstanceException("Instance state timeout reached '${Formats.duration(stateTime)}' for $instance!")
        }

        if (runner.runningTime >= constant) {
            throw InstanceException("Instance constant timeout reached '${Formats.duration(runner.runningTime)}'!")
        }
    }
}