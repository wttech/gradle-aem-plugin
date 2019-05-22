package com.cognifide.gradle.aem.instance.content

import com.cognifide.gradle.aem.instance.InstanceSync

class Nodes(val sync: InstanceSync) {

    fun get(path: String): Node? {
        return Node.load(sync, path)
    }

    fun create(path: String, props: Map<String, Any>): Node {
        return Node.create(sync, path, props)
    }

    fun update(path: String, props: Map<String, Any>): Node {
        return Node.update(sync, path, props)
    }

    fun save(path: String, props: Map<String, Any>): Node {
        return Node.createOrUpdate(sync, path, props)
    }

    fun remove(path: String) {
        return Node.delete(sync, path)
    }

    fun exists(path: String): Boolean {
        return Node.exists(sync, path)
    }
}
