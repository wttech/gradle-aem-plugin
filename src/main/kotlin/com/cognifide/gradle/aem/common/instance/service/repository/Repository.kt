package com.cognifide.gradle.aem.common.instance.service.repository

import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
import java.io.File

class Repository(sync: InstanceSync) : InstanceService(sync) {

    /**
     * Take care about property value types saved in repository.
     */
    val typeHints = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("instance.repository.typeHints")?.let { set(it) }
    }

    /**
     * Controls level of logging. By default repository related operations are only logged at debug level.
     * This switch could increase logging level to info level.
     */
    val verboseLogging = aem.obj.boolean {
        convention(false)
        aem.prop.boolean("instance.repository.verboseLogging")?.let { set(it) }
    }

    /**
     * Controls throwing exceptions in case of response statuses indicating repository errors.
     * Switching it to false, allows custom error handling in task scripting.
     */
    val responseChecks = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("instance.repository.responseChecks")?.let { set(it) }
    }

    /**
     * When trying to upload file under '/content/dam', repository will use for upload dedicated AEM service
     * instead of using Sling service.
     */
    val damUploads = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("instance.repository.damUploads")?.let { set(it) }
    }

    /**
     * Manipulate node at given path (CRUD).
     */
    fun node(path: String): Node = Node(this, path)

    /**
     * Calculate a value using node at given path (e.g read property and return it).
     */
    fun <T> node(path: String, options: Node.() -> T): T = node(path).run(options)

     /**
     * Shorthand method for creating or updating node at given path.
     */
    fun node(path: String, properties: Map<String, Any?>): RepositoryResult {
         val (dir, name) = splitPath(path)
         return when {
             name.contains(".") -> node(dir).import(properties, name, replace = true, replaceProperties = true)
             else -> node(path).save(properties)
         }
    }

    /**
     * Shorthand method for importing content from JSON file at given path.
     */
    fun node(path: String, jsonFile: File): RepositoryResult {
        val (dir, name) = splitPath(path)
        return node(dir).import(jsonFile, name, replace = true, replaceProperties = true)
    }

    private fun splitPath(path: String): Pair<String, String> {
        return path.substringBeforeLast("/") to path.substringAfterLast("/")
    }

    internal val http by lazy {
        RepositoryHttpClient(aem, instance).apply {
            responseChecks.set(this@Repository.responseChecks)
        }
    }
}
