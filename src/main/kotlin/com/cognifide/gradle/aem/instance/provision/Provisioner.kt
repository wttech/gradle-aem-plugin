package com.cognifide.gradle.aem.instance.provision

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.utils.Patterns
import com.cognifide.gradle.aem.common.utils.onEachApply

class Provisioner(val aem: AemExtension) {

    var instances = aem.instances

    private val steps = mutableListOf<Step>()

    fun step(id: String, options: Step.() -> Unit) {
        steps.add(Step(this, id).apply(options))
    }

    fun provision(stepName: String, greedy: Boolean) {
        steps.forEach { it.validate() }

        aem.parallel.each(instances) { instance ->
            steps.filter { Patterns.wildcard(it.id, stepName) }.map { InstanceStep(instance, it) }.onEachApply {
                if (greedy || isPerformable()) {
                    aem.logger.info("Provisioning step '${definition.id}' started at $instance")
                    perform()
                    aem.logger.info("Provisioning step '${definition.id}' ended at $instance")
                } else {
                    aem.logger.info("Provisioning step '${definition.id}' not performable on $instance")
                }
            }
        }
    }

}
