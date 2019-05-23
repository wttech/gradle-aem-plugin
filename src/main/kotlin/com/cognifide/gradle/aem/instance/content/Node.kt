package com.cognifide.gradle.aem.instance.content

import com.google.common.collect.ImmutableMap
import com.jayway.jsonpath.DocumentContext

class Node internal constructor(
    private val repository: Repository,
    private val document: DocumentContext,
    val path: String
) {

    val name: String
        get() = path.substringAfterLast("/")

    val json: String
        get() = document.jsonString()

    val props: Map<String, Any>
        get() = ImmutableMap.copyOf(document.json<LinkedHashMap<String, Any>>())

    val children: Sequence<Node>
        get() = repository.getChildren(this)

    fun property(propName: String): Any? = document.read(propName) as Any

    fun string(propName: String): String? = property(propName)?.toString()
}
