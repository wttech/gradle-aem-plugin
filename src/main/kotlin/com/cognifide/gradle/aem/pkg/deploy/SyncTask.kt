package com.cognifide.gradle.aem.pkg.deploy

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.instance.*
import com.cognifide.gradle.aem.instance.action.AwaitAction
import com.cognifide.gradle.aem.internal.PropertyParser
import org.gradle.api.tasks.Internal

abstract class SyncTask : AemDefaultTask() {

    @Internal
    protected val propertyParser = PropertyParser(project)

    protected fun synchronizeInstances(synchronizer: (InstanceSync) -> Unit) {
        synchronizeInstances(synchronizer, filterInstances())
    }

    protected fun <T : Instance> synchronizeInstances(synchronizer: (InstanceSync) -> Unit, instances: List<T>) {
        val callback = { instance: T -> synchronizeInstance(synchronizer, instance) }
        if (config.deployParallel) {
            instances.parallelStream().forEach(callback)
        } else {
            instances.onEach(callback)
        }
    }

    protected fun <T : Instance> synchronizeInstance(synchronizer: (InstanceSync) -> Unit, instance: T) {
        logger.info("Synchronizing with: $instance")

        synchronizer(InstanceSync(project, instance))
    }

    protected fun synchronizeLocalInstances(handler: (LocalHandle) -> Unit) {
        Instance.locals(project).forEach { instance -> synchronizeLocalInstance(handler, instance) }
    }

    protected fun synchronizeLocalInstance(handler: (LocalHandle) -> Unit, instance: LocalInstance) {
        logger.info("Synchronizing with: $instance")

        handler(LocalHandle(project, InstanceSync(project, instance)))
    }

    protected fun filterInstances(): List<Instance> {
        return Instance.filter(project)
    }

    protected fun filterInstances(instanceGroup: String): List<Instance> {
        return Instance.filter(project, instanceGroup)
    }

    protected fun awaitStableInstances() {
        AwaitAction(project, filterInstances()).perform()
    }

    protected fun awaitStableLocalInstances() {
        AwaitAction(project, Instance.locals(project)).perform()
    }

}