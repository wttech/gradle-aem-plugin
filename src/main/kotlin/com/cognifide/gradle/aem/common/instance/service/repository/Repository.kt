package com.cognifide.gradle.aem.common.instance.service.repository

import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
import org.apache.jackrabbit.vault.util.JcrConstants

class Repository(sync: InstanceSync) : InstanceService(sync) {

    internal val http = RepositoryHttpClient(aem, instance)

    /**
     * Take care about property value types saved in repository.
     */
    var typeHints: Boolean = aem.props.boolean("instance.repository.typeHints") ?: true

    /**
     * Controls throwing exceptions in case of response statuses indicating repository errors.
     * Switching it to false, allows custom error handling in task scripting.
     */
    var responseChecks: Boolean
        get() = http.responseChecks
        set(value) {
            http.responseChecks = value
        }

    init {
        responseChecks = aem.props.boolean("instance.repository.responseChecks") ?: true
    }

    /**
     * Controls level of logging. By default repository related operations are only logged at debug level.
     * This switch could increase logging level to info level.
     */
    var verboseLogging: Boolean = aem.props.boolean("instance.repository.verboseLogging") ?: false

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
    fun node(path: String, properties: Map<String, Any?>): RepositoryResult = node(path).save(properties)

}
