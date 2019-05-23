package com.cognifide.gradle.aem.instance.service.repository

import com.cognifide.gradle.aem.common.http.RequestException
import com.cognifide.gradle.aem.instance.InstanceService
import com.cognifide.gradle.aem.instance.InstanceSync
import com.jayway.jsonpath.DocumentContext

class Repository(sync: InstanceSync) : InstanceService(sync) {

    fun getNode(path: String): Node {
        try {
            return Node(this, loadNode(path), path)
        } catch (e: RequestException) {
            throw RepositoryException("Unable to load Node: $path", e)
        }
    }

    fun findNode(path: String): Node? {
        var node: Node?
        try {
            node = getNode(path)
        } catch (e: RepositoryException) {
            node = null
        }
        return node
    }

    fun createNode(path: String, props: Map<String, Any?>): Node {
        try {
            sync.post(path, props.handleNulls())
            return getNode(path)
        } catch (e: RequestException) {
            throw RepositoryException("Unable to create Node: $path", e)
        }
    }

    fun updateNode(path: String, props: Map<String, Any?>): Node {
        return if (hasNode(path)) {
            sync.post(path, props.handleNulls())
            getNode(path)
        } else {
            throw RepositoryException("Unable to update Node: $path. Node doesn't exists.")
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
            throw RepositoryException("Unable to delete Node: $path", e)
        }
    }

    fun hasNode(path: String): Boolean {
        var node: Node?
        try {
            node = getNode(path)
        } catch (e: RepositoryException) {
            node = null
        }
        return node != null
    }

    fun getProperty(path: String, propName: String): Any {
        return findProperty(path, propName)
        ?: throw RepositoryException("Unable to load Property: $path/$propName")
    }

    fun findProperty(path: String, propName: String): Any? {
        return Node(this, loadNode(path), path).property(propName)
    }

    fun updateProperty(path: String, propName: String, value: Any): Node {
        return updateNode(path, mapOf(propName to value))
    }

    fun removeProperty(path: String, propName: String): Node {
        return updateNode(path, mapOf("$propName@Delete" to "this prop will be deleted"))
    }

    fun hasProperty(path: String, propName: String): Boolean {
        return getNode(path).property(propName) != null
    }

    private fun loadNode(path: String): DocumentContext = sync.get("$path.json") { asJson(it) }

    private fun Map<String, Any?>.handleNulls(): Map<String, Any?> {
        return this
                .mapKeys { if (it.value == null) "${it.key}@Delete" else it.key }
                .mapValues { if (it.value == null) "this prop will be deleted" else it.value }
    }
}
