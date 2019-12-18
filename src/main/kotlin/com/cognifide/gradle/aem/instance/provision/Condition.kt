package com.cognifide.gradle.aem.instance.provision

import com.cognifide.gradle.aem.common.utils.Formats
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

@Suppress("FunctionOnlyReturningConstant")
class Condition(val step: InstanceStep) {

    val instance = step.instance

    // Partial conditions

    fun always(): Boolean = true

    fun never(): Boolean = false

    fun rerunOnFail(): Boolean = step.ended && step.failed && step.definition.rerunOnFail

    fun sinceEndedMoreThan(millis: Long) = step.ended && !Formats.durationFit(step.endedAt.time, instance.zoneId, millis)

    // Complete conditions

    /**
     * Perform step only once, but try again if it fails.
     */
    fun once() = failSafeOnce()

    /**
     * Perform step only once, but try again if it fails.
     */
    fun failSafeOnce(): Boolean = ultimateOnce() || rerunOnFail()

    /**
     * Perform step only once regardless if it fails or not.
     */
    fun ultimateOnce() = !step.ended

    /**
     * Repeat performing step after specified number of milliseconds since last time.
     */
    fun repeatAfter(millis: Long): Boolean = failSafeOnce() || sinceEndedMoreThan(millis)

    /**
     * Repeat performing step after specified number of seconds since last time.
     */
    fun repeatAfterSeconds(count: Long) = repeatAfter(TimeUnit.SECONDS.toMillis(count))

    /**
     * Repeat performing step after specified number of minutes since last time.
     */
    fun repeatAfterMinutes(count: Long) = repeatAfter(TimeUnit.MINUTES.toMillis(count))

    /**
     * Repeat performing step after specified number of hours since last time.
     */
    fun repeatAfterHours(count: Long) = repeatAfter(TimeUnit.HOURS.toMillis(count))

    /**
     * Repeat performing step after specified number of days since last time.
     */
    fun repeatAfterDays(count: Long) = repeatAfter(TimeUnit.DAYS.toMillis(count))

    /**
     * Repeat performing step basing on counter based predicate.
     */
    fun repeatEvery(counterPredicate: (Long) -> Boolean) = counterPredicate(step.counter)

    /**
     * Repeat performing step every n-times.
     */
    fun repeatEvery(times: Long) = repeatEvery { counter -> counter % times == 0L }

    /**
     * Repeat performing step with a probability specified as percentage [0, 1.0).
     */
    fun repeatProbably(probability: Double) = ThreadLocalRandom.current().nextDouble() <= probability
}
