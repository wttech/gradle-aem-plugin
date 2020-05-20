package com.cognifide.gradle.aem.common.instance.provision.step

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.instance.provision.ProvisionException
import com.cognifide.gradle.aem.common.instance.provision.Provisioner

class CustomStep(provisioner: Provisioner, id: String) : AbstractStep(provisioner, id) {

    override fun validate() {
        if (!::actionCallback.isInitialized) {
            throw ProvisionException("Step '$id' action is not defined!")
        }
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
