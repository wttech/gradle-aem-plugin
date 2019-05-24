package com.cognifide.gradle.aem.common.instance.service.repository

import java.io.Serializable
import net.minidev.json.JSONArray

class Node internal constructor(private val repository: Repository, val path: String) : Serializable {

    private val document = repository.sync.get("$path.json") { asJson(it) }

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

    fun property(propName: String): Any? = document.read(propName)

    fun string(propName: String): String? = property(propName)?.toString()

    override fun toString(): String {
        return "Node(path='$path')"
    }
}
