package com.cognifide.gradle.sling.common.instance

import com.cognifide.gradle.sling.SlingExtension
import com.cognifide.gradle.sling.common.instance.service.osgi.OsgiFramework
import com.cognifide.gradle.sling.common.instance.service.pkg.PackageManager
import com.cognifide.gradle.sling.common.instance.service.repository.Repository
import com.cognifide.gradle.sling.common.instance.service.crx.Crx
import com.cognifide.gradle.sling.common.instance.service.status.Status

class InstanceSync(val sling: SlingExtension, val instance: Instance) {

    val http by lazy { InstanceHttpClient(sling, instance) }

    /**
     * Perform easily any HTTP requests to Sling instance.
     */
    fun <T> http(callback: InstanceHttpClient.() -> T): T = http.run(callback)

    val osgiFramework by lazy { OsgiFramework(this) }

    val osgi get() = osgiFramework

    /**
     * Control OSGi framework (Apache Felix) on Sling instance.
     */
    fun <T> osgiFramework(callback: OsgiFramework.() -> T): T = osgiFramework.run(callback)

    val packageManager by lazy { PackageManager(this) }

    /**
     * Manage CRX packages on Sling instance (upload, install, delete etc).
     */
    fun <T> packageManager(callback: PackageManager.() -> T): T = packageManager.run(callback)

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

    // TODO rename / use composum endpoints instead
    val crx by lazy { Crx(this) }

    /**
     * CRX DE Endpoints accessor (node types etc).
     */
    fun <T> crx(callback: Crx.() -> T): T = crx.run(callback)

    init {
        sling.instanceManager.syncOptions(this)
    }
}
