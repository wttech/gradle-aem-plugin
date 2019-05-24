package com.cognifide.gradle.aem.common.instance.service.repository

import com.cognifide.gradle.aem.common.http.RequestException
import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync

class Repository(sync: InstanceSync) : InstanceService(sync) {

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
            sync.post(path, props.handleNulls())
            return getNode(path)
        } catch (e: RequestException) {
            throw RepositoryException("Unable to create node: $path", e)
        }
    }

    fun updateNode(path: String, props: Map<String, Any?>): Node {
        return if (hasNode(path)) {
            sync.post(path, props.handleNulls())
            getNode(path)
        } else {
            throw RepositoryException("Unable to update node: $path. Node does not exist.")
        }
    }

    fun saveNode(path: String, props: Map<String, Any?>): Node {
        return if (hasNode(path)) {
            sync.post(path, props.handleNulls())
            getNode(path)
        } else {
            createNode(path, props.handleNulls())
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

    private fun Map<String, Any?>.handleNulls(): Map<String, Any?> {
        return this
                .mapKeys { if (it.value == null) "${it.key}@Delete" else it.key }
                .mapValues { if (it.value == null) "" else it.value }
    }
}
