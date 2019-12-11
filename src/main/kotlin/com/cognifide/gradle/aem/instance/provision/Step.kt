package com.cognifide.gradle.aem.instance.provision

import com.cognifide.gradle.aem.common.build.Retry
import com.cognifide.gradle.aem.common.instance.Instance

class Step(val provisioner: Provisioner, val id: String) {

    private val aem = provisioner.aem

    internal lateinit var actionCallback: Instance.() -> Unit

    var conditionCallback: Condition.() -> Boolean = { once() }

    /**
     * Nice name of step describing purpose.
     */
    var description: String? = null

    /**
     * Allows to redo step action after delay if exception is thrown.
     */
    var retry: Retry = aem.retry { afterSquaredSecond(aem.prop.long("instance.provision.step.retry") ?: 0L) }

    /**
     * Controls logging error to console instead of breaking build with exception so that next step might be performed.
     */
    var continueOnFail: Boolean = aem.prop.boolean("instance.provision.step.continueOnFail") ?: false

    /**
     * Controls if step should be performed again when previously failed.
     */
    var rerunOnFail: Boolean = aem.prop.boolean("instance.provision.step.rerunOnFail") ?: true

    fun validate() {
        if (!::actionCallback.isInitialized) {
            throw ProvisionException("Step '$id' action is not defined!")
        }
    }

    fun action(callback: Instance.() -> Unit) {
        this.actionCallback = callback
    }

    fun condition(callback: Condition.() -> Boolean) {
        this.conditionCallback = callback
    }

    fun retry(options: Retry.() -> Unit) {
        this.retry = aem.retry(options)
    }

    override fun toString(): String {
        return "Step(id='$id', description=$description, continueOnFail=$continueOnFail, rerunOnFail=$rerunOnFail)"
    }
}
