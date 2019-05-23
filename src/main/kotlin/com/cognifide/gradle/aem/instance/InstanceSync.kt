package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.instance.service.StateChecker
import com.cognifide.gradle.aem.instance.service.groovy.GroovyConsole
import com.cognifide.gradle.aem.instance.service.osgi.OsgiFramework
import com.cognifide.gradle.aem.instance.service.pkg.PackageManager
import com.cognifide.gradle.aem.instance.service.repository.Repository

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
}