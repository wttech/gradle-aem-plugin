package com.cognifide.gradle.aem.instance.provision

import com.cognifide.gradle.aem.AemDefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

open class InstanceProvision : AemDefaultTask() {

    /**
     * Forces to perform all steps regardless their state on instances (already performed).
     */
    @Input
    var greedy = aem.props.flag("instance.provision.greedy")

    /**
     * Determines which steps should be performed selectively.
     */
    @Input
    var stepName = aem.props.string("instance.provision.step") ?: "*"

    private val provisioner = Provisioner(aem)

    @TaskAction
    fun provision() {
        provisioner.provision(stepName, greedy)
    }

    fun provisioner(options: Provisioner.() -> Unit) {
        provisioner.apply(options)
    }

    fun step(id: String, options: Step.() -> Unit) = provisioner.step(id, options)

    companion object {

        const val NAME = "instanceProvision"
    }
}
