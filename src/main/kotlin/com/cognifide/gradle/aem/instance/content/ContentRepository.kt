package com.cognifide.gradle.aem.instance.content

import com.cognifide.gradle.aem.instance.InstanceSync

class ContentRepository(private val sync: InstanceSync) {

    fun getNode(path: String): Node {
        return Node.load(sync, path)
    }

    fun findNode(path: String): Node? {
        var node: Node?
        try {
            node = getNode(path)
        } catch (e: NodeException){
            node = null
        }
        return node
    }

    fun createNode(path: String, props: Map<String, Any>): Node {
        return Node.create(sync, path, props)
    }

    fun updateNode(path: String, props: Map<String, Any>): Node {
        return Node.update(sync, path, props)
    }

    fun saveNode(path: String, props: Map<String, Any>): Node {
        return Node.createOrUpdate(sync, path, props)
    }

    fun removeNode(path: String) {
        return Node.delete(sync, path)
    }

    fun hasNode(path: String): Boolean {
        return Node.exists(sync, path)
    }
}
