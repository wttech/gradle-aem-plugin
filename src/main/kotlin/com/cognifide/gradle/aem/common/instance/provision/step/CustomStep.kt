package com.cognifide.gradle.aem.common.instance.provision.step

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.instance.provision.ProvisionException
import com.cognifide.gradle.aem.common.instance.provision.Provisioner
import com.cognifide.gradle.aem.common.instance.provision.Step

class CustomStep(provisioner: Provisioner) : Step(provisioner) {

    fun validate(callback: () -> Unit) {
        this.validateCallback = callback
    }

    private var validateCallback: () -> Unit = {}

    override fun validate() {
        if (!id.isPresent) {
            throw ProvisionException("Step ID is not defined!")
        }
        if (!::actionCallback.isInitialized) {
            throw ProvisionException("Step '${id.get()}' action is not defined!")
        }

        validateCallback()
    }

    fun init(callback: () -> Unit) {
        this.initCallback = callback
    }

    private var initCallback: () -> Unit = {}

    override fun init() {
        initCallback()
    }

    fun action(callback: Instance.() -> Unit) {
        this.actionCallback = callback
    }

    private lateinit var actionCallback: Instance.() -> Unit

    override fun action(instance: Instance) {
        actionCallback(instance)
    }

    fun sync(callback: InstanceSync.() -> Unit) = action { sync(callback) }
}
