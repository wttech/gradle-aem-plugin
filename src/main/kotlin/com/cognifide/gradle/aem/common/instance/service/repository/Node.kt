package com.cognifide.gradle.aem.common.instance.service.repository

import com.cognifide.gradle.aem.common.http.RequestException
import com.cognifide.gradle.aem.common.utils.Formats
import com.fasterxml.jackson.annotation.JsonIgnore
import com.jayway.jsonpath.PathNotFoundException
import java.io.Serializable
import java.util.*
import net.minidev.json.JSONArray
import org.apache.commons.lang3.StringUtils
import org.apache.jackrabbit.vault.util.JcrConstants

class Node(private val repository: Repository, val path: String) : Serializable {

    val name: String
        get() = path.substringAfterLast("/")

    private var propertiesLoaded: Properties? = null

    val properties: Properties
        get() = propertiesLoaded ?: reloadProperties()

    @get:JsonIgnore
    val type: String
        get() = properties.string(JcrConstants.JCR_PRIMARYTYPE)!!

    @get:JsonIgnore
    val parent: Node
        get() = Node(repository, path.substringBeforeLast("/"))

    @get:JsonIgnore
    val children: List<Node>
        get() = children().toList()

    @Suppress("unchecked_cast")
    fun children(): Sequence<Node> = repository.sync.get("$path.harray.1.json") { asJson(it) }
            .run {
                try {
                    read<JSONArray>(Property.CHILDREN.value)
                } catch (e: PathNotFoundException) {
                    JSONArray() // no children
                }
            }
            .map { child -> child as Map<String, Any> }
            .map { props ->
                Node(repository, "$path/${props[Property.NAME.value]}").apply {
                    propertiesLoaded = Properties(filterMetaProperties(props))
                }
            }
            .asSequence()

    @get:JsonIgnore
    val json: String
        get() = Formats.toJson(this)

    fun save(properties: Map<String, Any?>) {
        try {
            repository.sync.post(path, preparePostProperties(properties))
        } catch (e: RequestException) {
            throw RepositoryException("Cannot save repository node: $path", e)
        }
    }

    fun delete() {
        try {
            repository.sync.delete(path)
        } catch (e: RequestException) {
            throw RepositoryException("Cannot delete repository node: $path", e)
        }
    }

    fun reload() {
        reloadProperties()
    }

    fun saveProperty(name: String, value: Any?) = save(mapOf(name to value))

    fun deleteProperty(name: String) = saveProperty(name, null)

    fun hasProperty(name: String): Boolean = properties[name] != null

    private fun reloadProperties(): Properties {
        return repository.sync.get("$path.json") {
            Properties(asJson(it).json<LinkedHashMap<String, Any>>()).apply { propertiesLoaded = this }
        }
    }

    /**
     * Implementation for supporting "Controlling Content Updates with @ Suffixes"
     * @see <https://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html>
     *
     * TODO support 'repository.typeHints'
     */
    private fun preparePostProperties(properties: Map<String, Any?>): Map<String, Any?> {
        return properties.entries.fold(mutableMapOf(), { p, (n, v) ->
            when {
                repository.nullDeletes && v == null -> p[StringUtils.appendIfMissing(n, "@Delete")] = ""
                else -> p[n] = v
            }
            p
        })
    }

    private fun filterMetaProperties(properties: Map<String, Any>): Map<String, Any> {
        return properties.filterKeys { p -> !Property.values().any { it.value == p } }
    }

    fun recurse(filter: (Node) -> Boolean) = recurse(filter, false)

    fun recurse(filter: (Node) -> Boolean = { true }, self: Boolean = false): Sequence<Node> = sequence {
        val stack = Stack<Node>()

        if (self) {
            stack.add(this@Node)
        } else {
            stack.addAll(children)
        }

        while (stack.isNotEmpty()) {
            val current = stack.pop()
            if (filter(current)) {
                stack.addAll(current.children)
                yield(current)
            }
        }
    }

    enum class Property(val value: String) {
        CHILDREN("__children__"),
        NAME("__name__")
    }

    override fun toString(): String {
        return "Node(path='$path', type='$type', properties=$properties)"
    }
}
