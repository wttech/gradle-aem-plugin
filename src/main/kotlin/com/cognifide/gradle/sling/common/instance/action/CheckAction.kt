package com.cognifide.gradle.sling.common.instance.action

import com.cognifide.gradle.sling.SlingExtension
import com.cognifide.gradle.sling.common.instance.Instance
import com.cognifide.gradle.sling.common.instance.check.CheckRunner
import com.cognifide.gradle.sling.common.instance.names

/**
 * Verify instances using custom runner and set of checks.
 */
class CheckAction(sling: SlingExtension) : DefaultAction(sling) {

    val runner = CheckRunner(sling)

    fun runner(options: CheckRunner.() -> Unit) {
        runner.apply(options)
    }

    override fun perform(instances: Collection<Instance>) {
        if (instances.isEmpty()) {
            logger.info("No instances to check.")
            return
        }

        logger.info("Checking instance(s): ${instances.names}")

        runner.check(instances)
    }
}
