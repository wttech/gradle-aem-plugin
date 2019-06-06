package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.service.groovy.GroovyConsole
import com.cognifide.gradle.aem.common.instance.service.osgi.OsgiFramework
import com.cognifide.gradle.aem.common.instance.service.pkg.PackageManager
import com.cognifide.gradle.aem.common.instance.service.repository.Repository
import com.cognifide.gradle.aem.common.instance.service.status.StatusRetriever

class InstanceSync(val aem: AemExtension, val instance: Instance) {

    val http = InstanceHttpClient(aem, instance)

    /**
     * Perform easily any HTTP requests to AEM instance.
     */
    fun <T> http(callback: InstanceHttpClient.() -> T): T = http.run(callback)

    val osgiFramework = OsgiFramework(this)

    /**
     * Control OSGi framework (Apache Felix) on AEM instance.
     */
    fun <T> osgiFramework(callback: OsgiFramework.() -> T): T = osgiFramework.run(callback)

    val packageManager = PackageManager(this)

    /**
     * Manage CRX packages on AEM instance (upload, install, delete etc).
     */
    fun <T> packageManager(callback: PackageManager.() -> T): T = packageManager.run(callback)

    val groovyConsole = GroovyConsole(this)

    /**
     * Execute Groovy Scripts on AEM runtime.
     */
    fun <T> groovyConsole(callback: GroovyConsole.() -> T): T = groovyConsole.run(callback)

    val repository = Repository(this)

    /**
     * Perform operations on JCR content repository (CRUD etc).
     */
    fun <T> repository(callback: Repository.() -> T): T = repository.run(callback)

    var status = StatusRetriever(this)

    /**
     * Status retriever (system properties etc).
     */
    fun <T> status(callback: StatusRetriever.() -> T): T = status.run(callback)
}