package com.cognifide.gradle.aem.instance.content

interface ContentManipulator {

    fun getNode(path: String): Node?

    fun createNode(path: String, props: Map<String, Any> = mapOf()): Node

    fun updateNode(path: String, props: Map<String, Any> = mapOf()): Node

    fun saveNode(path: String, props: Map<String, Any> = mapOf()): Node

    fun removeNode(path: String)

    fun hasNode(path: String): Boolean
}

