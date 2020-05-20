package com.cognifide.gradle.aem.common.instance.provision

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import com.cognifide.gradle.common.build.Retry

class Step(val provisioner: Provisioner, val id: String) {

    private val aem = provisioner.aem

    private val common = aem.common

    internal lateinit var actionCallback: Instance.() -> Unit

    private var initCallback: () -> Unit = {}

    var conditionCallback: Condition.() -> Boolean = { once() }

    /**
     * Nice name of step describing purpose.
     */
    val description = aem.obj.string()

    /**
     * Description if set, ID otherwise.
     */
    val label get() = description.get() ?: id

    /**
     * Implementation version.
     */
    var version = aem.obj.string { convention(InstanceStep.VERSION_DEFAULT) }

    /**
     * Controls logging error to console instead of breaking build with exception so that next step might be performed.
     */
    val continueOnFail = aem.obj.boolean {
        convention(false)
        aem.prop.boolean("instance.provision.step.continueOnFail")?.let { set(it) }
    }

    /**
     * Controls if step should be performed again when previously failed.
     */
    val rerunOnFail = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("instance.provision.step.rerunOnFail")?.let { set(it) }
    }

    /**
     * Allows to redo step action after delay if exception is thrown.
     */
    var retry: Retry = common.retry { afterSquaredSecond(aem.prop.long("instance.provision.step.retry") ?: 0L) }

    /**
     * Controls is after running step on all instances, checking for up instances need to be done.
     */
    val awaitUp = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("instance.provision.step.awaitUp")?.let { set(it) }
    }

    private var awaitOptionally = false

    private var awaitRequired = false

    fun awaitIf(callback: () -> Boolean) {
        awaitOptionally = true
        if (callback()) {
            awaitRequired = true
        }
    }

    private var awaitUpOptions: AwaitUpAction.() -> Unit = {}

    fun awaitUp(options: AwaitUpAction.() -> Unit) {
        this.awaitUpOptions = options
    }

    fun awaitUp(instances: Collection<Instance>) {
        if (awaitUp.get() && (!awaitOptionally || awaitRequired)) {
            aem.instanceManager.awaitUp(instances, awaitUpOptions)
        }
    }

    fun validate() {
        if (!::actionCallback.isInitialized) {
            throw ProvisionException("Step '$id' action is not defined!")
        }
    }

    fun init() {
        initCallback()
    }

    fun init(callback: () -> Unit) {
        this.initCallback = callback
    }

    fun action(callback: Instance.() -> Unit) {
        this.actionCallback = callback
    }

    fun sync(callback: InstanceSync.() -> Unit) = action { sync(callback) }

    fun condition(callback: Condition.() -> Boolean) {
        this.conditionCallback = callback
    }

    fun retry(options: Retry.() -> Unit) {
        this.retry = common.retry(options)
    }

    override fun toString(): String = "Step(id='$id', description=${description.get()}, " +
            "continueOnFail=${continueOnFail.get()}, rerunOnFail=${rerunOnFail.get()})"
}
