package com.cognifide.gradle.aem.common.instance.provision.step

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.provision.ProvisionException
import com.cognifide.gradle.aem.common.instance.provision.Provisioner
import com.cognifide.gradle.aem.common.instance.service.repository.ReplicationAgent

class ConfigureReplicationAgentStep(provisioner: Provisioner, val location: String, val name: String) : AbstractStep(provisioner) {

    lateinit var agentAction: ReplicationAgent.() -> Unit

    fun agent(action: ReplicationAgent.() -> Unit) {
        this.agentAction = action
    }

    override fun action(instance: Instance) {
        instance.sync {
            if (!::agentAction.isInitialized) {
                throw ProvisionException("Step '${id.get()}' has no replication agent action defined!")
            }
            repository.replicationAgent(location, name).apply(agentAction)
        }
    }

    init {
        id.set("configureReplicationAgent/$location/$name")
        description.convention("Configuring replication agent on '$location' named '$name'")
    }
}
