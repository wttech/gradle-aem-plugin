package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.service.StateChecker
import com.cognifide.gradle.aem.common.instance.service.groovy.GroovyConsole
import com.cognifide.gradle.aem.common.instance.service.osgi.OsgiFramework
import com.cognifide.gradle.aem.common.instance.service.pkg.PackageManager
import com.cognifide.gradle.aem.common.instance.service.repository.Repository

class InstanceSync(aem: AemExtension, instance: Instance) : InstanceHttpClient(aem, instance) {

    fun stateChecker() = StateChecker(this)

    val osgiFramework = OsgiFramework(this)

    fun osgiFramework(callback: OsgiFramework.() -> Unit) {
        osgiFramework.apply(callback)
    }

    val packageManager = PackageManager(this)

    fun packageManager(callback: PackageManager.() -> Unit) {
        packageManager.apply(callback)
    }

    val groovyConsole = GroovyConsole(this)

    fun groovyConsole(callback: GroovyConsole.() -> Unit) {
        groovyConsole.apply(callback)
    }

    val repository = Repository(this)

    fun repository(callback: Repository.() -> Unit) {
        repository.apply(callback)
    }

    fun customize(options: InstanceSync.() -> Unit): InstanceSync {
        return InstanceSync(aem, instance).apply(options)
    }
}