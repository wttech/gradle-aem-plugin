package com.cognifide.gradle.aem.common.instance.action

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.check.CheckRunner
import com.cognifide.gradle.aem.common.instance.names

/**
 * Verify instances using custom runner and set of checks.
 */
class CheckAction(aem: AemExtension) : DefaultAction(aem) {

    val runner = CheckRunner(aem)

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
