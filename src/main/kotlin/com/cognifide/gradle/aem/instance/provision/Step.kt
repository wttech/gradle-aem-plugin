package com.cognifide.gradle.aem.instance.provision

import com.cognifide.gradle.aem.common.instance.Instance

class Step(val provisioner: Provisioner, val id: String) {

    private val aem = provisioner.aem

    internal lateinit var actionCallback: Instance.() -> Unit

    var conditionCallback: Condition.() -> Boolean = { once() }

    var description: String? = null

    var continueOnFail: Boolean = false

    var rerunOnFail: Boolean = true

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

    override fun toString(): String {
        return "Step(id='$id', description=$description, continueOnFail=$continueOnFail, rerunOnFail=$rerunOnFail)"
    }
}
