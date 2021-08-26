package com.cognifide.gradle.aem.common.instance.provision.step

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.provision.ProvisionException
import com.cognifide.gradle.aem.common.instance.provision.Provisioner
import com.cognifide.gradle.aem.common.instance.service.repository.ReplicationAgent

class ConfigureReplicationAgentStep(provisioner: Provisioner, val location: String, val name: String) : AbstractStep(provisioner) {

    lateinit var agentAction: ReplicationAgent.() -> Unit

    val bundleToggle = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("instance.provision.configureReplicationAgent.bundleToggle")?.let { set(it) }
    }

    val bundleSymbolicName = aem.obj.string {
        convention("com.day.cq.cq-replication")
        aem.prop.string("instance.provision.configureReplicationAgent.bundleSymbolicName")?.let { set(it) }
    }

    fun agent(action: ReplicationAgent.() -> Unit) {
        this.agentAction = action
    }

    fun agent(props: Map<String, Any?>) {
        agent { configure(props) }
        version(props)
    }

    override fun validate() {
        if (!::agentAction.isInitialized) {
            throw ProvisionException("Step '${id.get()}' has no replication agent action defined!")
        }
    }

    override fun action(instance: Instance) {
        instance.sync {
            if (bundleToggle.get()) {
                osgi.toggleBundle(bundleSymbolicName.get()) {
                    repository.replicationAgent(location, name).apply(agentAction)
                }
            } else {
                repository.replicationAgent(location, name).apply(agentAction)
            }
        }
    }

    init {
        id.set("configureReplicationAgent/$location/$name")
        description.convention("Configuring replication agent on '$location' named '$name'")
    }
}
