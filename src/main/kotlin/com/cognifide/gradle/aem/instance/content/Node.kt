package com.cognifide.gradle.aem.instance.content

import com.jayway.jsonpath.DocumentContext
import net.minidev.json.JSONArray

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
        get() = document.json<LinkedHashMap<String, Any>>().toMap()

    val children: Sequence<Node>
        get() = repository.sync.get("$path.harray.1.json") { asJson(it) }
                .read<JSONArray>("__children__")
                .map { child -> child as Map<*, *> }
                .map { props -> repository.getNode("$path/${props["__name__"]}") }
                .asSequence()

    fun property(propName: String): Any? = document.read(propName) as Any

    fun string(propName: String): String? = property(propName)?.toString()
}
