package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.common.instance.InstanceException
import java.util.concurrent.TimeUnit

class HelpCheck(group: CheckGroup) : DefaultCheck(group) {

    /**
     * After longer inactivity time, try helping instance going back to healthy state.
     */
    val stateTime = aem.obj.long { convention(TimeUnit.MINUTES.toMillis(8)) }

    override fun check() {
        if (progress.stateTime >= stateTime.get()) {
            progress.stateData.getOrPut(STATE_HELPED) {
                help()
                true
            }
        }
    }

    private fun help() {
        throw InstanceException("Trying to help instance!")
    }

    companion object {
        private const val STATE_HELPED = "helped"
    }
}
