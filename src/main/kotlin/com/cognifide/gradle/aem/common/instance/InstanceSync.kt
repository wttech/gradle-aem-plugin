package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.service.groovy.GroovyConsole
import com.cognifide.gradle.aem.common.instance.service.osgi.OsgiFramework
import com.cognifide.gradle.aem.common.instance.service.pkg.PackageManager
import com.cognifide.gradle.aem.common.instance.service.repository.Repository
import com.cognifide.gradle.aem.common.instance.service.crx.Crx
import com.cognifide.gradle.aem.common.instance.service.status.Status
import com.cognifide.gradle.aem.common.instance.service.workflow.WorkflowManager

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

    val status = Status(this)

    /**
     * Status retriever (system properties, product version etc).
     */
    fun <T> status(callback: Status.() -> T): T = status.run(callback)

    var workflowManager = WorkflowManager(this)

    /**
     * Perform operations on workflows (enabling, disabling)
     */
    fun <T> workflowManager(callback: WorkflowManager.() -> T) = workflowManager.run(callback)

    val crx = Crx(this)

    /**
     * CRX DE Endpoints accessor (node types etc).
     */
    fun <T> crx(callback: Crx.() -> T): T = crx.run(callback)

    init {
        aem.instanceManager.syncOptions(this)
    }
}
