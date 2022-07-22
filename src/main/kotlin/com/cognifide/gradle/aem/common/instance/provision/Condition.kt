package com.cognifide.gradle.aem.common.instance.provision

import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.common.utils.Patterns
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

@Suppress("FunctionOnlyReturningConstant")
class Condition(val step: InstanceStep) {

    val instance = step.instance

    // Partial conditions

    fun always(): Boolean = true

    fun never(): Boolean = false

    fun greedy(): Boolean = step.greedy

    fun rerunOnFail(): Boolean = step.ended && step.failed && step.definition.rerunOnFail.get()

    fun sinceEndedMoreThan(millis: Long) = step.ended && !Formats.durationFit(step.endedAt.time, instance.zoneId, millis)

    fun onEnv(env: String) = Patterns.wildcard(instance.env, env)

    fun onImage() = step.definition.provisioner.aem.commonOptions.envImage

    fun onInstance(name: String) = Patterns.wildcard(instance.name, name)

    fun onAuthor() = instance.author

    fun onPublish() = instance.publish

    fun onLocal() = instance.local

    fun onRemote() = !onLocal()

    fun onRunMode(vararg modes: String) = onRunMode(modes.asIterable())

    fun onRunMode(modes: Iterable<String>, except: Iterable<String> = listOf()) = instance.runningModes.run {
        containsAll(modes.toList()) && none { except.contains(it) }
    }

    // Complete conditions

    /**
     * Perform step only once, but try again if it fails.
     */
    fun once() = greedy() || failSafeOnce()

    /**
     * Perform step only once, but try again if it fails.
     */
    fun failSafeOnce(): Boolean = ultimateOnce() || rerunOnFail()

    /**
     * Perform step only once regardless if it fails or not.
     */
    fun ultimateOnce() = !step.ended || step.changed

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
