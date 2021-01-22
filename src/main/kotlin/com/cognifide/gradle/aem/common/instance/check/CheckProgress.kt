package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.common.utils.Patterns
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

    val abbreviatedSummary: String get() {
        val sign = currentCheck?.let { if (it.done) "+" else "-" } ?: "~"
        val parts = mutableListOf<String>()

        currentCheck?.summary?.let { summary ->
            if (Patterns.wildcard(summary, "* (*)")) {
                val text = summary.substringBefore(" (")
                val number = summary.substringAfter(" (").removeSuffix(")")
                parts.add("$sign${text.firstLetters()}")
                parts.add(number)
            } else {
                parts.add("$sign${summary.firstLetters()}")
            }
        }

        return "${instance.name}: ${parts.joinToString(" ")}"
    }

    private fun String.firstLetters() = this.split(" ").mapNotNull { it.firstOrNull() }.joinToString("")
}
