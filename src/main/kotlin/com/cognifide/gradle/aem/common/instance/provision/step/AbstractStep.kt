package com.cognifide.gradle.aem.common.instance.provision.step

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import com.cognifide.gradle.aem.common.instance.provision.*
import org.apache.commons.lang3.builder.HashCodeBuilder

abstract class AbstractStep(final override val provisioner: Provisioner) : Step {

    protected val aem = provisioner.aem

    protected val logger = aem.logger

    protected val common = aem.common

    override val id = aem.obj.string()

    override val description = aem.obj.string()

    override var version = aem.obj.string { convention(InstanceStep.VERSION_DEFAULT) }

    override fun version(vararg dependencies: Any?) {
        version.set(aem.obj.provider {
            val builder = HashCodeBuilder()
            dependencies.forEach { builder.append(it) }
            builder.build().toString()
        })
    }

    override fun isPerformable(condition: Condition): Boolean {
        val operation = "condition provision step '${id.get()}' for '${condition.instance.name}'"
        return conditionRetry.withCountdown<Boolean, Exception>(operation) { conditionCallback(condition) }
    }

    override fun condition(callback: Condition.() -> Boolean) {
        this.conditionCallback = callback
    }

    var conditionCallback: Condition.() -> Boolean = { once() }

    override val continueOnFail = aem.obj.boolean {
        convention(false)
        aem.prop.boolean("instance.provision.step.continueOnFail")?.let { set(it) }
    }

    override val rerunOnFail = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("instance.provision.step.rerunOnFail")?.let { set(it) }
    }

    override val actionRetry = common.retry { afterSquaredSecond(aem.prop.long("instance.provision.step.actionRetry") ?: 1L) }

    override val conditionRetry = common.retry { afterSquaredSecond(aem.prop.long("instance.provision.step.conditionRetry") ?: 3L) }

    override val awaitUp = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("instance.provision.step.awaitUp")?.let { set(it) }
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
            aem.instanceManager.awaitUp(instances, awaitUpOptions)
        }
    }

    override fun init() {
        // nothing to do
    }

    override fun validate() {
        // nothing to do
    }

    override fun toString(): String = "${javaClass.simpleName}(id=${id.get()}, version=${version.get()})"
}
