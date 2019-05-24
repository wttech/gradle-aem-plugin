package com.cognifide.gradle.aem.common.instance.service.repository

import com.cognifide.gradle.aem.common.http.RequestException
import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
import org.apache.commons.lang3.StringUtils

class Repository(sync: InstanceSync) : InstanceService(sync) {

    val typeHints: Boolean = true

    val nullDeletes: Boolean = true

    fun getNode(path: String): Node {
        try {
            return Node(this, path)
        } catch (e: RequestException) {
            throw RepositoryException("Unable to load repository node: $path", e)
        }
    }

    fun findNode(path: String): Node? = try {
        getNode(path)
    } catch (e: RepositoryException) {
        null
    }

    fun createNode(path: String, props: Map<String, Any?>): Node {
        try {
            sync.post(path, normalizeProperties(props))
            return getNode(path)
        } catch (e: RequestException) {
            throw RepositoryException("Unable to create node: $path", e)
        }
    }

    fun updateNode(path: String, props: Map<String, Any?>): Node {
        return if (hasNode(path)) {
            sync.post(path, normalizeProperties(props))
            getNode(path)
        } else {
            throw RepositoryException("Unable to update node: $path. Node does not exist.")
        }
    }

    fun saveNode(path: String, props: Map<String, Any?>): Node {
        return if (hasNode(path)) {
            sync.post(path, normalizeProperties(props))
            getNode(path)
        } else {
            createNode(path, props)
        }
    }

    fun removeNode(path: String) {
        try {
            sync.delete(path)
        } catch (e: RequestException) {
            throw RepositoryException("Unable to delete node: $path", e)
        }
    }

    fun hasNode(path: String): Boolean = findNode(path) != null

    fun getProperty(path: String, name: String): Any {
        return findProperty(path, name) ?: throw RepositoryException("Unable to load node property: $path[$name]")
    }

    fun findProperty(path: String, name: String): Any? = Node(this, path).property(name)

    fun updateProperty(path: String, name: String, value: Any?): Node = updateNode(path, mapOf(name to value))

    fun removeProperty(path: String, name: String): Node = updateProperty(path, name, null)

    fun hasProperty(path: String, name: String): Boolean = findNode(path)?.property(name) != null

    private fun normalizeProperties(properties: Map<String, Any?>): Map<String, Any?> {
        return properties.entries.fold(mutableMapOf(), { p, (n, v) ->
            when {
                nullDeletes && v == null -> p[StringUtils.appendIfMissing(n, "@Delete")] = ""
                else -> p[n] = v
            }
            p
        })
    }
}
