package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.utils.shortenClass
import com.cognifide.gradle.common.utils.Patterns
import com.cognifide.gradle.common.utils.toLowerSnakeCase
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

    val summaryAbbreviated: String get() {
        val sign = currentCheck?.let { if (it.done) "+" else "-" } ?: "~"
        val parts = mutableListOf<String>()

        currentCheck?.summary?.let { summary ->
            when {
                Patterns.wildcard(summary, "* (*)") -> {
                    val text = summary.substringBefore(" (")
                    val number = summary.substringAfter(" (").removeSuffix(")")
                    parts.add("$sign${text.firstLetters()}")
                    parts.add(number)
                }
                Patterns.wildcard(summary, "* '*'") -> {
                    val text = summary.substringBefore(" '")
                    val subText = summary.substringAfter(" '").removeSuffix("'")
                    parts.add("$sign${text.firstLetters()}")
                    parts.add(subText.shortenClass())
                }
                else -> parts.add("$sign${summary.firstLetters()}")
            }
        }

        val instanceName = (
            instance.env.get().toLowerSnakeCase().replace("_", " ") + " " +
                instance.id.get().toLowerSnakeCase().replace("_", " ")
            ).firstLetters()

        return "$instanceName: ${parts.joinToString(" ")}"
    }

    private fun String.firstLetters() = this.split(" ").mapNotNull { it.firstOrNull() }.joinToString("")
}
