package com.cognifide.gradle.aem.pkg.deploy

import com.cognifide.gradle.aem.base.api.AemDefaultTask
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.InstanceActions
import com.cognifide.gradle.aem.instance.InstanceSync
import com.cognifide.gradle.aem.instance.LocalHandle
import com.cognifide.gradle.aem.internal.PropertyParser
import org.gradle.api.tasks.Internal

abstract class SyncTask : AemDefaultTask() {

    @Internal
    protected val propertyParser = PropertyParser(project)

    protected fun synchronizeInstances(synchronizer: (InstanceSync) -> Unit) {
        synchronizeInstances(synchronizer, filterInstances())
    }

    protected fun <T : Instance> synchronizeInstances(synchronizer: (InstanceSync) -> Unit, instances: List<T>) {
        val callback = { instance: T -> synchronizeInstances(synchronizer, instance) }
        if (config.deployParallel) {
            instances.parallelStream().forEach(callback)
        } else {
            instances.onEach(callback)
        }
    }

    protected fun <T : Instance> synchronizeInstances(synchronizer: (InstanceSync) -> Unit, instance: T) {
        logger.info("Synchronizing with: $instance")

        synchronizer(InstanceSync(project, instance))
    }

    protected fun synchronizeLocalInstances(handler: (LocalHandle) -> Unit) {
        Instance.locals(project).forEach { instance ->
            handler(LocalHandle(project, InstanceSync(project, instance)))
        }
    }

    protected fun filterInstances(instanceGroup: String = Instance.FILTER_LOCAL): List<Instance> {
        return Instance.filter(project, instanceGroup)
    }

    protected fun awaitStableInstances() {
        InstanceActions(project).awaitStable(filterInstances())
    }

    protected fun awaitStableLocalInstances() {
        InstanceActions(project).awaitStable(Instance.locals(project))
    }

}