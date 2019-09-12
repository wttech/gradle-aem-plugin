package com.cognifide.gradle.aem.instance.provision

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceException

class Step(val provisioner: Provisioner, val id: String) {

    private val aem = provisioner.aem

    internal lateinit var actionCallback: Instance.() -> Unit

    internal lateinit var conditionCallback: Condition.() -> Boolean

    fun validate() {
        if (!::actionCallback.isInitialized) {
            throw InstanceException("Step '$id' action is not defined!")
        }

        if (!::conditionCallback.isInitialized) {
            throw InstanceException("Step '$id' condition is not defined!")
        }
    }

    fun action(callback: Instance.() -> Unit) {
        this.actionCallback = callback
    }

    fun condition(callback: Condition.() -> Boolean) {
        this.conditionCallback = callback
    }

}
