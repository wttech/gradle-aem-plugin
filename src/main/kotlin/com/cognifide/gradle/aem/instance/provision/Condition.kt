package com.cognifide.gradle.aem.instance.provision

import com.cognifide.gradle.aem.common.utils.Formats
import java.util.concurrent.TimeUnit

class Condition(val step: InstanceStep) {

    val metadata = step.metadata

    val instance = step.instance

    fun always(): Boolean = true

    fun never(): Boolean = false

    fun once(): Boolean = !step.done

    fun every(times: Long) = every { counter -> counter % times == 0L }

    fun every(counterPredicate: (Long) -> Boolean) = counterPredicate(metadata.counter)

    fun afterMillis(millis: Long): Boolean = once() || Formats.timeUp(step.endedAt.time, instance.zoneId, millis)

    fun afterDays(count: Long) = afterMillis(TimeUnit.DAYS.toMillis(count))

    fun afterHours(count: Long) = afterMillis(TimeUnit.HOURS.toMillis(count))

    fun afterMinutes(count: Long) = afterMillis(TimeUnit.MINUTES.toMillis(count))

    fun afterSeconds(count: Long) = afterMillis(TimeUnit.SECONDS.toMillis(count))
}
