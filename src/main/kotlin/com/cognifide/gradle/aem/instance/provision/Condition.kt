package com.cognifide.gradle.aem.instance.provision

import com.cognifide.gradle.aem.common.utils.Formats
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class Condition(val step: InstanceStep) {

    val instance = step.instance

    fun once(): Boolean = !step.done

    fun afterTime(millis: Long): Boolean {
        if (!step.done) {
            return true
        }

        val nowTimestamp = LocalDateTime.now().atZone(ZoneId.systemDefault())
        val thenTimestamp = Formats.dateTime(step.endedAt.time, step.instance.zoneId)
        val diffMillis = ChronoUnit.MILLIS.between(thenTimestamp, nowTimestamp)

        return diffMillis < millis
    }

    fun afterDays(count: Long) = afterTime(TimeUnit.DAYS.toMillis(count))

    fun afterHours(count: Long) = afterTime(TimeUnit.HOURS.toMillis(count))

    fun afterMinutes(count: Long) = afterTime(TimeUnit.MINUTES.toMillis(count))

    fun afterSeconds(count: Long) = afterTime(TimeUnit.SECONDS.toMillis(count))
}
