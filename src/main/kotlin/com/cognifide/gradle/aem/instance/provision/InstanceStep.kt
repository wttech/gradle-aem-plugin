package com.cognifide.gradle.aem.instance.provision

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceException
import java.util.*

class InstanceStep(val instance: Instance, val definition: Step) {

    private val aem = definition.provisioner.aem

    private val repository = instance.sync.repository

    private val marker = repository.node("/var/gap/${aem.project.rootProject.name}/provision/step/${definition.id}")

    val startedAt: Date
        get() = marker.properties.date(STARTED_PROP) ?: throw InstanceException("Step not yet started!")

    val started: Boolean
        get() = marker.exists && marker.hasProperty(STARTED_PROP)

    val endedAt: Date
        get() = marker.properties.date(ENDED_PROP) ?: throw InstanceException("Step not yet ended!")

    val ended: Boolean
        get() = marker.exists && marker.hasProperty(ENDED_PROP)

    val done: Boolean
        get() = ended

    val executionTime: Long
        get() = marker.properties.long("executionTime") ?: -1L

    fun isPerformable(): Boolean {
        return definition.conditionCallback(Condition(this))
    }

    fun perform() {
        marker.saveProperty(STARTED_PROP, Date())
        definition.actionCallback(instance)
        marker.saveProperty(ENDED_PROP, Date())
    }

    companion object {
        const val STARTED_PROP = "started"

        const val ENDED_PROP = "ended"
    }

}
