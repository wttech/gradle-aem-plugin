package com.cognifide.gradle.sling.common.instance.check

import com.cognifide.gradle.sling.common.instance.Instance
import org.apache.commons.lang3.time.StopWatch

class CheckProgress(val instance: Instance) {

    var currentCheck: CheckGroup? = null

    var previousCheck: CheckGroup? = null

    val stateChanged: Boolean
        get() {
            val current = currentCheck ?: return true
            val previous = previousCheck ?: return true

            return current.state != previous.state
        }

    var stateChanges = 0

    internal var stateWatch = StopWatch()

    val stateTime: Long get() = stateWatch.time

    val stateData = mutableMapOf<String, Any>()

    val summary: String get() = "${instance.name}: ${currentCheck?.summary ?: "In progress"}"

    val shortSummary: String get() = "${instance.name}: ${currentCheck?.status ?: "In progress"}"
}
