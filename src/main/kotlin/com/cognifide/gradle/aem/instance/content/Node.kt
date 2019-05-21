package com.cognifide.gradle.aem.instance.content

import com.cognifide.gradle.aem.instance.InstanceSync
import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import net.minidev.json.JSONArray
import org.apache.jackrabbit.vault.util.JcrConstants

class Node private constructor() {

    lateinit var sync: InstanceSync
    lateinit var path: String
    lateinit var name: String
    lateinit var document: DocumentContext

    fun children(): Iterator<Node> {
        return sync.get("$path.harray.1.json") { asJson(it) }
                .read<JSONArray>("__children__")
                .map { props -> loadChild(sync, path, props as MutableMap<*, *>) }
                .iterator()
    }

    fun get(propName: String): Any = document.read(propName) as Any
    fun getString(propName: String): String = document.read(propName) as String
    fun json(): String = document.jsonString()

    fun getJcrTitle(): String = getString(JcrConstants.JCR_TITLE)
    fun getJcrCreated() = get(JcrConstants.JCR_CREATED)
    fun getJcrCreatedBy(): String = getString(JcrConstants.JCR_CREATED_BY)
    fun getJcrDescription(): String = getString(JcrConstants.JCR_DESCRIPTION)
    fun getJcrPrimaryType(): String = getString(JcrConstants.JCR_PRIMARYTYPE)
    fun getSlingResourceType(): String = getString("sling:resourceType")
    fun getCqAllowedTemplates() = get("cq:allowedTemplates")
    fun getCqDesignPath(): String = getString("cq:designPath")
    fun getCqTemplate(): String = getString("cq:template")

    private fun initMainProps(instanceSync: InstanceSync, nodePath: String, nodeName: String) {
        sync = instanceSync
        path = nodePath
        name = nodeName
    }

    companion object {
        fun load(instanceSync: InstanceSync, nodePath: String): Node = Node().apply {
            initMainProps(instanceSync, nodePath, nodePath.substringAfterLast("/"))

            try {
                document = sync.get("$path.json") { asJson(it) }
            } catch (e: Exception) {
                throw NodeException("Unable to load JCR Node: $nodePath", e)
            }
        }

        fun loadChild(instanceSync: InstanceSync, parentNodePath: String, props: MutableMap<*, *>): Node = Node().apply {
            val childName = props.remove("__name__") as String
            initMainProps(instanceSync, "$parentNodePath/$childName", "$parentNodePath/$childName")

            document = JsonPath.parse(props)
        }

        fun create(instanceSync: InstanceSync, nodePath: String, props: Map<String, Any> = mapOf()): Node = Node().apply {
            initMainProps(instanceSync, nodePath, nodePath.substringAfterLast("/"))

            try {
                sync.post("$path.json", props)
                document = sync.get("$path.json") { asJson(it) }
            } catch (e: Exception) {
                throw NodeException("Unable to create JCR Node: $nodePath", e)
            }
        }

        fun update(instanceSync: InstanceSync, nodePath: String, props: Map<String, Any> = mapOf()): Node = Node().apply {
            initMainProps(instanceSync, nodePath, nodePath.substringAfterLast("/"))

            try {
                document = sync.get("$path.json") { asJson(it) }
                sync.post("$path.json", props)
            } catch (e: Exception) {
                throw NodeException("Unable to update JCR Node: $nodePath (does it exist?)", e)
            }
        }

        fun createOrUpdate(instanceSync: InstanceSync, nodePath: String, props: Map<String, Any> = mapOf()): Node = Node().apply {
            return if (exists(instanceSync, nodePath)) {
                update(instanceSync, nodePath, props)
            } else {
                create(instanceSync, nodePath, props)
            }
        }

        fun delete(instanceSync: InstanceSync, nodePath: String) {
            try {
                instanceSync.delete(nodePath)
            } catch (e: Exception) {
                throw NodeException("Unable to delete JCR Node: $nodePath", e)
            }
        }

        fun exists(instanceSync: InstanceSync, nodePath: String): Boolean {
            var node: Node?
            try {
                node = load(instanceSync, nodePath)
            } catch (e: Exception) {
                node = null
            }
            return node != null
        }
    }
}
