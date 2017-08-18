package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.instance.*
import com.cognifide.gradle.aem.internal.PropertyParser
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested

abstract class SyncTask : DefaultTask(), AemTask {

    @Nested
    final override val config = AemConfig.of(project)

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
        InstanceActions.awaitStable(project, filterInstances())
    }

    protected fun awaitStableLocalInstances() {
        InstanceActions.awaitStable(project, Instance.locals(project))
    }

}