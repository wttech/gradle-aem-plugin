package com.cognifide.gradle.aem.pkg.deploy

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.InstanceSync
import com.cognifide.gradle.aem.instance.LocalHandle
import com.cognifide.gradle.aem.instance.LocalInstance
import com.cognifide.gradle.aem.instance.action.AwaitAction

abstract class SyncTask : AemDefaultTask() {

    protected fun synchronizeInstances(synchronizer: (InstanceSync) -> Unit) {
        synchronizeInstances(synchronizer, Instance.filter(project))
    }

    protected fun <T : Instance> synchronizeInstances(synchronizer: (InstanceSync) -> Unit, instances: List<T>) {
        synchronizeInstances(instances, { instance: T -> synchronizeInstance(synchronizer, instance) })
    }

    protected fun <T : Instance> synchronizeInstances(instances: List<T>, callback: (T) -> Unit) {
        if (instances.isEmpty()) {
            logger.warn("No instances to synchronize. Verify deploy instance name filter '${config.deployInstanceName}' and defined instances name (environment-type).")
        } else {
            if (config.deployParallel) {
                logger.info("Synchronizing ${instances.size} instance(s) in parallel mode")
                instances.parallelStream().forEach(callback)
            } else {
                logger.info("Synchronizing ${instances.size} instance(s) in sequential mode")
                instances.onEach(callback)
            }
        }
    }

    protected fun <T : Instance> synchronizeInstance(synchronizer: (InstanceSync) -> Unit, instance: T) {
        logger.info("Synchronizing with: $instance")

        synchronizer(InstanceSync(project, instance))
    }

    protected fun synchronizeLocalInstances(handler: (LocalHandle) -> Unit) {
        synchronizeInstances(Instance.locals(project), { instance -> synchronizeLocalInstance(handler, instance) })
    }

    protected fun synchronizeLocalInstance(handler: (LocalHandle) -> Unit, instance: LocalInstance) {
        logger.info("Synchronizing with: $instance")

        handler(LocalHandle(project, instance))
    }

    protected fun awaitStableInstances() {
        AwaitAction(project, Instance.filter(project)).perform()
    }

    protected fun awaitStableLocalInstances() {
        AwaitAction(project, Instance.locals(project)).perform()
    }

}