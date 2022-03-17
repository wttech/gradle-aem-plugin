package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.aem
import com.cognifide.gradle.aem.common.CommonPlugin
import com.cognifide.gradle.aem.instance.tasks.InstanceProvision
import com.cognifide.gradle.aem.instance.tasks.InstanceRcp
import com.cognifide.gradle.aem.instance.tasks.InstanceTail
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.common.*
import com.cognifide.gradle.common.java.JavaSupport
import com.cognifide.gradle.common.tasks.runtime.*
import com.cognifide.gradle.common.utils.onEachApply
import org.gradle.api.Project

class LocalInstancePlugin : CommonDefaultPlugin() {

    override fun Project.configureProject() {
        setupDependentPlugins()
        setupTasks()
        setupJavaEnforcement()
    }

    private fun Project.setupDependentPlugins() {
        if (plugins.hasPlugin(PackagePlugin::class.java)) {
            throw AemException("Local instance plugin '${InstancePlugin.ID}' must be applied before package plugin '${PackagePlugin.ID}'!")
        }

        plugins.apply(InstancePlugin::class.java)
        plugins.apply(RuntimePlugin::class.java)
    }

    @Suppress("LongMethod")
    private fun Project.setupTasks() = tasks {

        // Plugin tasks

        val down = register<InstanceDown>(InstanceDown.NAME)
        val destroy = register<InstanceDestroy>(InstanceDestroy.NAME) {
            dependsOn(down)
        }.also { checkForce(it) }
        val resolve = register<InstanceResolve>(InstanceResolve.NAME)
        val create = register<InstanceCreate>(InstanceCreate.NAME) {
            mustRunAfter(destroy, resolve)
        }
        val up = register<InstanceUp>(InstanceUp.NAME) {
            dependsOn(create)
            mustRunAfter(down, destroy)
        }
        val restart = register<InstanceRestart>(InstanceRestart.NAME) {
            dependsOn(down, up)
        }
        named<InstanceProvision>(InstanceProvision.NAME) {
            mustRunAfter(resolve, create, up)
        }
        val await = named<InstanceAwait>(InstanceAwait.NAME) {
            mustRunAfter(create, up)
        }
        val setup = named<InstanceSetup>(InstanceSetup.NAME) {
            dependsOn(create, up)
            mustRunAfter(destroy)
        }
        val resetup = register<InstanceResetup>(InstanceResetup.NAME) {
            dependsOn(destroy, setup)
        }
        val backup = register<InstanceBackup>(InstanceBackup.NAME) {
            mustRunAfter(down)
        }
        named<InstanceTail>(InstanceTail.NAME) {
            mustRunAfter(resolve, create, up)
        }
        named<InstanceRcp>(InstanceRcp.NAME) {
            mustRunAfter(resolve, create, up)
        }
        named<InstanceGroovyEval>(InstanceGroovyEval.NAME) {
            mustRunAfter(resolve, create, up)
        }

        register<InstanceKill>(InstanceKill.NAME)

        // Runtime lifecycle

        named<Up>(Up.NAME) {
            dependsOn(up)
            mustRunAfter(backup)
        }
        named<Down>(Down.NAME) {
            dependsOn(down)
        }
        named<Destroy>(Destroy.NAME) {
            dependsOn(destroy)
        }
        named<Restart>(Restart.NAME) {
            dependsOn(restart)
        }
        named<Setup>(Setup.NAME) {
            dependsOn(setup)
        }
        named<Resetup>(Resetup.NAME) {
            dependsOn(resetup)
        }
        named<Resolve>(Resolve.NAME) {
            dependsOn(resolve)
        }
        named<Await>(Await.NAME) {
            dependsOn(await)
        }
    }

    private fun Project.setupJavaEnforcement() {
        val enabled = aem.prop.boolean("localInstance.javaEnforcement.enabled") ?: false
        if (!enabled) {
            return
        }

        gradle.projectsEvaluated {
            val enforcedVersion by lazy {
                val compatibleVersions = aem.localInstanceManager.determineJavaCompatibleVersions()
                val desiredVersion = compatibleVersions.lastOrNull()?.toString()
                val fallbackVersion = aem.prop.string("localInstance.javaEnforcement.version")
                    ?: aem.prop.string("javaSupport.version")
                    ?: JavaSupport.VERSION_DEFAULT

                desiredVersion ?: fallbackVersion
            }
            pluginProjects(CommonPlugin.ID).onEachApply {
                aem.common.javaSupport.version.set(provider { enforcedVersion })
            }
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.instance.local"
    }
}
