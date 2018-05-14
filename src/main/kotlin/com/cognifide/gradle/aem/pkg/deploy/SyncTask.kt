package com.cognifide.gradle.aem.pkg.deploy

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.InstanceSync
import com.cognifide.gradle.aem.instance.LocalHandle
import com.cognifide.gradle.aem.instance.LocalInstance

abstract class SyncTask : AemDefaultTask() {

    protected fun <T : Instance> synchronizeAnyInstances(instances: List<T>, callback: (T) -> Unit) {
        if (instances.isEmpty()) {
            logger.warn("No instances to synchronize. Verify deploy instance name filter '${config.instanceName}' and defined instances name (environment-type).")
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

    protected fun synchronizeInstances(synchronizer: (InstanceSync) -> Unit) {
        synchronizeInstances(Instance.filter(project), synchronizer)
    }

    protected fun <T : Instance> synchronizeInstances(instances: List<T>, synchronizer: (InstanceSync) -> Unit) {
        synchronizeAnyInstances(instances, { instance: T -> synchronizeInstance(synchronizer, instance) })
    }

    protected fun <T : Instance> synchronizeInstance(synchronizer: (InstanceSync) -> Unit, instance: T) {
        logger.info("Synchronizing with: $instance")

        synchronizer(InstanceSync(project, instance))
    }

    protected fun synchronizeLocalInstances(instances: List<LocalInstance>, handler: (LocalHandle) -> Unit) {
        synchronizeAnyInstances(instances, { instance -> synchronizeLocalInstance(handler, instance) })
    }

    protected fun synchronizeLocalInstance(handler: (LocalHandle) -> Unit, instance: LocalInstance) {
        logger.info("Synchronizing with: $instance")

        handler(LocalHandle(project, instance))
    }

}