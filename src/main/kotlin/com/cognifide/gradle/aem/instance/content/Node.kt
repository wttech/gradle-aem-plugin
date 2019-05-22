package com.cognifide.gradle.aem.instance.content

import com.cognifide.gradle.aem.common.http.RequestException
import com.cognifide.gradle.aem.instance.InstanceSync
import com.jayway.jsonpath.DocumentContext
import net.minidev.json.JSONArray
import org.apache.jackrabbit.vault.util.JcrConstants

class Node private constructor(val sync: InstanceSync, val path: String, val props: Map<String, Any> = mapOf()) {

    lateinit var document: DocumentContext

    val name: String
        get() = path.substringAfterLast("/")
    val jcrTitle: String
        get() = stringValue(JcrConstants.JCR_TITLE)
    val jcrCreated
        get() = value(JcrConstants.JCR_CREATED)
    val jcrCreatedBy: String
        get() = stringValue(JcrConstants.JCR_CREATED_BY)
    val jcrDescription: String
        get() = stringValue(JcrConstants.JCR_DESCRIPTION)
    val jcrPrimaryType: String
        get() = stringValue(JcrConstants.JCR_PRIMARYTYPE)
    val slingResourceType: String
        get() = stringValue("sling:resourceType")
    val cqAllowedTemplates
        get() = value("cq:allowedTemplates")
    val cqDesignPath: String
        get() = stringValue("cq:designPath")
    val cqTemplate: String
        get() = stringValue("cq:template")
    val children: Iterator<Node>
        get() = sync.get("$path.harray.1.json") { asJson(it) }
                .read<JSONArray>("__children__")
                .map { child -> child as Map<*, *> }
                .map { props -> load(sync, path + "/" + props["__name__"] as String) }
                .iterator()

    fun value(propName: String): Any = document.read(propName) as Any
    fun stringValue(propName: String): String = document.read(propName) as String
    fun json(): String = document.jsonString()

    companion object {
        fun load(sync: InstanceSync, path: String): Node = Node(sync, path).apply {
            try {
                document = sync.get("$path.json") { asJson(it) }
            } catch (e: RequestException) {
                throw NodeException("Unable to load JCR Node: $path", e)
            }
        }

        fun create(sync: InstanceSync, path: String, props: Map<String, Any>): Node = Node(sync, path).apply {
            try {
                sync.post(path, props)
                document = sync.get("$path.json") { asJson(it) }
            } catch (e: RequestException) {
                throw NodeException("Unable to create JCR Node: $path", e)
            }
        }

        fun update(sync: InstanceSync, path: String, props: Map<String, Any>): Node {
            return if (exists(sync, path)) {
                sync.post(path, props)
                load(sync, path)
            } else {
                throw NodeException("Unable to update JCR Node: $path. Node doesn't exist.")
            }
        }

        fun createOrUpdate(sync: InstanceSync, path: String, props: Map<String, Any>): Node {
            return if (exists(sync, path)) {
                sync.post(path, props)
                load(sync, path)
            } else {
                create(sync, path, props)
            }
        }

        fun delete(instanceSync: InstanceSync, nodePath: String) {
            try {
                instanceSync.delete(nodePath)
            } catch (e: RequestException) {
                throw NodeException("Unable to delete JCR Node: $nodePath", e)
            }
        }

        fun exists(instanceSync: InstanceSync, nodePath: String): Boolean {
            var node: Node?
            try {
                node = load(instanceSync, nodePath)
            } catch (e: NodeException) {
                node = null
            }
            return node != null
        }
    }
}
