package com.cognifide.gradle.aem.common.instance.provision

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import com.cognifide.gradle.aem.common.instance.action.ReloadAction
import com.cognifide.gradle.common.utils.Formats
import org.apache.commons.lang3.builder.HashCodeBuilder
import java.io.File

abstract class Step(val provisioner: Provisioner) {

    protected val aem = provisioner.aem

    protected val logger = aem.logger

    protected val common = aem.common

    /**
     * Short unique ID of step.
     */
    val id = aem.obj.string()

    /**
     * Description if set, ID otherwise.
     */
    val label get() = description.orNull ?: "Step \"${id.get()}\""

    /**
     * Nice name of step describing purpose.
     */
    val description = aem.obj.string()

    /**
     * Implementation version.
     */
    val version = aem.obj.string { convention(InstanceStep.VERSION_DEFAULT) }

    /**
     * Implementation version builder
     */
    fun version(vararg dependencies: Any?) {
        version.set(aem.obj.provider { versionFrom(dependencies.asIterable()) })
    }

    fun versionFrom(vararg dependencies: Any?) = versionFrom(dependencies.asIterable())

    fun versionFrom(dependencies: Iterable<Any?>): String {
        val builder = HashCodeBuilder()
        dependencies.forEach { dependency ->
            when (dependency) {
                is File -> {
                    builder.append(Formats.toChecksum(dependency))
                    builder.append(dependency.path)
                    builder.append(mapOf<String, String>().hashCode())
                }
                else -> builder.append(dependency)
            }
        }
        return builder.build().toString()
    }

    fun isPerformable(condition: Condition): Boolean {
        val operation = "condition provision step '${id.get()}' for '${condition.instance.name}'"
        return conditionRetry.withCountdown<Boolean, Exception>(operation) { conditionCallback(condition) }
    }

    /**
     * Controls if step should be performed in parallel on multiple instances at once.
     */
    val runInParallel = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("instance.provision.step.runInParallel")?.let { set(it) }
    }

    fun condition(callback: Condition.() -> Boolean) {
        this.conditionCallback = callback
    }

    var conditionCallback: Condition.() -> Boolean = { once() }

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
    val actionRetry = common.retry { afterSquaredSecond(aem.prop.long("instance.provision.step.actionRetry") ?: 3L) }

    /**
     * Allows to redo step condition after delay if exception is thrown.
     */
    val conditionRetry = common.retry { afterSquaredSecond(aem.prop.long("instance.provision.step.conditionRetry") ?: 3L) }

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

    /**
     * Controls if instances should be reloaded after applying step.
     * By default, only instance up state is awaited.
     */
    val reload = aem.obj.boolean {
        convention(false)
        aem.prop.boolean("instance.provision.step.reload")?.let { set(it) }
    }

    private var reloadOptions: ReloadAction.() -> Unit = {}

    fun awaitReload(options: ReloadAction.() -> Unit) {
        this.reloadOptions = options
    }

    fun awaitUp(instances: Collection<Instance>) {
        if (awaitUp.get() && (!awaitOptionally || awaitRequired)) {
            when {
                reload.get() -> aem.instanceManager.awaitReloaded(instances, reloadOptions, awaitUpOptions)
                else -> aem.instanceManager.awaitUp(instances, awaitUpOptions)
            }
        }
    }

    open fun init() {
        // nothing to do
    }

    open fun validate() {
        // nothing to do
    }

    abstract fun action(instance: Instance)

    override fun toString(): String = "${javaClass.simpleName}(id=${id.get()}, version=${version.get()})"
}
