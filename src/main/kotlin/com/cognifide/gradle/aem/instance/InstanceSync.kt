package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.instance.service.GroovyConsole
import com.cognifide.gradle.aem.instance.service.OsgiFramework
import com.cognifide.gradle.aem.instance.service.PackageManager
import com.cognifide.gradle.aem.instance.service.StateChecker

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
}