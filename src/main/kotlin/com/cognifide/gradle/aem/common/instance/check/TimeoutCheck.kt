package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.common.instance.InstanceException
import java.util.concurrent.TimeUnit

class TimeoutCheck(group: CheckGroup) : DefaultCheck(group) {

    var timeout: Long = TimeUnit.SECONDS.toMillis(60)

    override fun check() {
        if (runner.runningTime > timeout) {
            throw InstanceException("Instance timeout reached!")
        }
    }
}