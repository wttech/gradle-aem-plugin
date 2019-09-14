package com.cognifide.gradle.aem.instance.provision

import com.cognifide.gradle.aem.common.utils.Formats
import java.util.concurrent.TimeUnit

class Condition(val step: InstanceStep) {

    val instance = step.instance

    // Partial conditions

    fun always(): Boolean = true

    fun never(): Boolean = false

    fun rerunOnFail(): Boolean = step.ended && step.failed && step.definition.rerunOnFail

    fun sinceEndedMoreThan(millis: Long) = step.ended && !Formats.durationFit(step.endedAt.time, instance.zoneId, millis)

    // Complete conditions

    fun once() = failSafeOnce()

    fun ultimateOnce() = !step.ended

    fun failSafeOnce(): Boolean = ultimateOnce() || rerunOnFail()

    fun repeatAfter(millis: Long): Boolean = failSafeOnce() || sinceEndedMoreThan(millis)

    fun repeatAfterDays(count: Long) = repeatAfter(TimeUnit.DAYS.toMillis(count))

    fun repeatAfterHours(count: Long) = repeatAfter(TimeUnit.HOURS.toMillis(count))

    fun repeatAfterMinutes(count: Long) = repeatAfter(TimeUnit.MINUTES.toMillis(count))

    fun repeatAfterSeconds(count: Long) = repeatAfter(TimeUnit.SECONDS.toMillis(count))

    fun repeatEvery(counterPredicate: (Long) -> Boolean) = counterPredicate(step.counter)

    fun repeatEvery(times: Long) = repeatEvery { counter -> counter % times == 0L }
}
