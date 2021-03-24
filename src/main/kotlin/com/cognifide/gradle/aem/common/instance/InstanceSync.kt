package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.service.auth.AuthManager
import com.cognifide.gradle.aem.common.instance.service.groovy.GroovyConsole
import com.cognifide.gradle.aem.common.instance.service.osgi.OsgiFramework
import com.cognifide.gradle.aem.common.instance.service.pkg.PackageManager
import com.cognifide.gradle.aem.common.instance.service.repository.Repository
import com.cognifide.gradle.aem.common.instance.service.crx.Crx
import com.cognifide.gradle.aem.common.instance.service.sling.SlingInstaller
import com.cognifide.gradle.aem.common.instance.service.status.Status
import com.cognifide.gradle.aem.common.instance.service.workflow.WorkflowManager

class InstanceSync(val aem: AemExtension, val instance: Instance) {

    val http by lazy { InstanceHttpClient(aem, instance) }

    /**
     * Perform easily any HTTP requests to AEM instance.
     */
    fun <T> http(callback: InstanceHttpClient.() -> T): T = http.run(callback)

    val osgiFramework by lazy { OsgiFramework(this) }

    val osgi get() = osgiFramework

    /**
     * Control OSGi framework (Apache Felix) on AEM instance.
     */
    fun <T> osgiFramework(callback: OsgiFramework.() -> T): T = osgiFramework.run(callback)

    val packageManager by lazy { PackageManager(this) }

    /**
     * Manage CRX packages on AEM instance (upload, install, delete etc).
     */
    fun <T> packageManager(callback: PackageManager.() -> T): T = packageManager.run(callback)

    val groovyConsole by lazy { GroovyConsole(this) }

    /**
     * Execute Groovy Scripts on AEM runtime.
     */
    fun <T> groovyConsole(callback: GroovyConsole.() -> T): T = groovyConsole.run(callback)

    val repository by lazy { Repository(this) }

    /**
     * Perform operations on JCR content repository (CRUD etc).
     */
    fun <T> repository(callback: Repository.() -> T): T = repository.run(callback)

    val status by lazy { Status(this) }

    /**
     * Status retriever (system properties, product version etc).
     */
    fun <T> status(callback: Status.() -> T): T = status.run(callback)

    val workflowManager by lazy { WorkflowManager(this) }

    /**
     * Perform operations on workflows (enabling, disabling)
     */
    fun <T> workflowManager(callback: WorkflowManager.() -> T) = workflowManager.run(callback)

    val crx by lazy { Crx(this) }

    /**
     * CRX DE endpoints accessor (node types etc).
     */
    fun <T> crx(callback: Crx.() -> T): T = crx.run(callback)

    val authManager by lazy { AuthManager(this) }

    /**
     * Performs authorization related operations like changing passwords.
     */
    fun <T> authManager(callback: AuthManager.() -> T): T = authManager.run(callback)

    val slingInstaller by lazy { SlingInstaller(this) }

    /**
     * JMX / monitoring endpoints accessor.
     */
    fun <T> slingInstaller(callback: SlingInstaller.() -> T): T = slingInstaller.run(callback)

    init {
        aem.instanceManager.syncOptions(this)
    }
}
