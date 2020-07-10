package com.cognifide.gradle.sling.common.instance.provision.step

import com.cognifide.gradle.sling.common.instance.Instance
import com.cognifide.gradle.sling.common.instance.action.AwaitUpAction
import com.cognifide.gradle.sling.common.instance.provision.*

abstract class AbstractStep(final override val provisioner: Provisioner) : Step {

    protected val sling = provisioner.sling

    protected val logger = sling.logger

    protected val common = sling.common

    override val id = sling.obj.string()

    override val description = sling.obj.string()

    override var version = sling.obj.string { convention(InstanceStep.VERSION_DEFAULT) }

    override fun condition(condition: Condition) = conditionCallback(condition)

    override fun condition(callback: Condition.() -> Boolean) {
        this.conditionCallback = callback
    }

    var conditionCallback: Condition.() -> Boolean = { once() }

    override val continueOnFail = sling.obj.boolean {
        convention(false)
        sling.prop.boolean("instance.provision.step.continueOnFail")?.let { set(it) }
    }

    override val rerunOnFail = sling.obj.boolean {
        convention(true)
        sling.prop.boolean("instance.provision.step.rerunOnFail")?.let { set(it) }
    }

    override val retry = common.retry { afterSquaredSecond(sling.prop.long("instance.provision.step.retry") ?: 0L) }

    override val awaitUp = sling.obj.boolean {
        convention(true)
        sling.prop.boolean("instance.provision.step.awaitUp")?.let { set(it) }
    }

    private var awaitOptionally = false

    private var awaitRequired = false

    override fun awaitIf(callback: () -> Boolean) {
        awaitOptionally = true
        if (callback()) {
            awaitRequired = true
        }
    }

    private var awaitUpOptions: AwaitUpAction.() -> Unit = {}

    override fun awaitUp(options: AwaitUpAction.() -> Unit) {
        this.awaitUpOptions = options
    }

    override fun awaitUp(instances: Collection<Instance>) {
        if (awaitUp.get() && (!awaitOptionally || awaitRequired)) {
            sling.instanceManager.awaitUp(instances, awaitUpOptions)
        }
    }

    override fun validate() {
        // nothing to do
    }

    override fun toString(): String = "${javaClass.simpleName}(id=${id.get()}, version=${version.get()})"
}
