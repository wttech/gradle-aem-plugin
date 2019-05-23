package com.cognifide.gradle.aem.instance.content

import com.jayway.jsonpath.DocumentContext
import org.apache.jackrabbit.vault.util.JcrConstants

class Node internal constructor(val repository: Repository, val document: DocumentContext, val path: String, val props: Map<String, Any> = mapOf()) {

    val name: String
        get() = path.substringAfterLast("/")

    val jcrTitle: String?
        get() = string(JcrConstants.JCR_TITLE)

    val jcrCreated: Any?
        get() = property(JcrConstants.JCR_CREATED)

    val jcrCreatedBy: String?
        get() = string(JcrConstants.JCR_CREATED_BY)

    val jcrDescription: String?
        get() = string(JcrConstants.JCR_DESCRIPTION)

    val jcrPrimaryType: String?
        get() = string(JcrConstants.JCR_PRIMARYTYPE)

    val children: Sequence<Node>
        get() = repository.getChildren(this)

    fun property(propName: String): Any? = document.read(propName) as Any

    fun string(propName: String): String? = property(propName)?.toString()

    fun json(): String = document.jsonString()
}
