package com.cognifide.gradle.aem.common.instance.provision

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import com.cognifide.gradle.aem.common.instance.action.ReloadAction
import com.cognifide.gradle.common.build.Retry
import org.gradle.api.provider.Property

interface Step {

    val provisioner: Provisioner

    /**
     * Short unique ID of step.
     */
    val id: Property<String>

    /**
     * Nice name of step describing purpose.
     */
    val description: Property<String>

    /**
     * Description if set, ID otherwise.
     */
    val label get() = description.orNull ?: "Step \"${id.get()}\""

    /**
     * Implementation version.
     */
    val version: Property<String>

    /**
     * Implementation version builder
     */
    fun version(vararg dependencies: Any?)

    /**
     * Controls logging error to console instead of breaking build with exception so that next step might be performed.
     */
    val continueOnFail: Property<Boolean>

    /**
     * Controls if step should be performed again when previously failed.
     */
    val rerunOnFail: Property<Boolean>

    /**
     * Controls if instances should be reloaded after applying step.
     * By default, only instance up state is awaited.
     */
    val reload: Property<Boolean>

    /**
     * Allows to redo step action after delay if exception is thrown.
     */
    val actionRetry: Retry

    /**
     * Allows to redo step condition after delay if exception is thrown.
     */
    val conditionRetry: Retry

    /**
     * Controls is after running step on all instances, checking for up instances need to be done.
     */
    val awaitUp: Property<Boolean>

    fun awaitIf(callback: () -> Boolean)

    fun awaitUp(options: AwaitUpAction.() -> Unit)

    fun awaitReload(options: ReloadAction.() -> Unit)

    fun condition(callback: Condition.() -> Boolean)

    fun validate()

    fun init()

    fun isPerformable(condition: Condition): Boolean

    fun action(instance: Instance)

    fun awaitUp(instances: Collection<Instance>)
}
