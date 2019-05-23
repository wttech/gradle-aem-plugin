package com.cognifide.gradle.aem.instance.content

import com.cognifide.gradle.aem.common.http.RequestException
import com.cognifide.gradle.aem.instance.InstanceService
import com.cognifide.gradle.aem.instance.InstanceSync
import com.jayway.jsonpath.DocumentContext
import net.minidev.json.JSONArray

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

    fun createNode(path: String, props: Map<String, Any>): Node {
        try {
            sync.post(path, props)
            return getNode(path)
        } catch (e: RequestException) {
            throw RepositoryException("Unable to create Node: $path", e)
        }
    }

    fun updateNode(path: String, props: Map<String, Any>): Node {
        return if (hasNode(path)) {
            sync.post(path, props)
            getNode(path)
        } else {
            throw RepositoryException("Unable to update Node: $path. Node doesn't exists.")
        }
    }

    fun saveNode(path: String, props: Map<String, Any>): Node {
        return if (hasNode(path)) {
            sync.post(path, props)
            getNode(path)
        } else {
            createNode(path, props)
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

    fun hasProperty(path: String, propName: String): Boolean {
        return getNode(path).property(propName) != null
    }

    internal fun getChildren(node: Node): Sequence<Node> {
        return sync.get("${node.path}.harray.1.json") { asJson(it) }
                .read<JSONArray>("__children__")
                .map { child -> child as Map<*, *> }
                .map { props -> getNode("${node.path}${node.path}/${props["__name__"]}") }
                .asSequence()
    }

    private fun loadNode(path: String): DocumentContext = sync.get("$path.json") { asJson(it) }
}
