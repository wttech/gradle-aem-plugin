package com.cognifide.gradle.aem.common.instance.service.repository

import com.cognifide.gradle.aem.common.http.RequestException
import com.cognifide.gradle.aem.common.utils.Formats
import com.fasterxml.jackson.annotation.JsonIgnore
import com.jayway.jsonpath.PathNotFoundException
import java.io.Serializable
import java.util.*
import net.minidev.json.JSONArray
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
    fun children(): Sequence<Node> = repository.http.get("$path.harray.1.json") { asJson(it) }
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

    fun save(properties: Map<String, Any?>, removeUnspecified: Boolean = false): RepositoryResult = try {
        val allProps = postProperties(properties, removeUnspecified) + operationProperties("")
        repository.http.postMultipart(path, allProps) {
            asObjectFromJson(it, RepositoryResult::class.java)
        }
    } catch (e: RequestException) {
        throw RepositoryException("Cannot save repository node: $path", e)
    }

    fun delete(): RepositoryResult = try {
        repository.http.postMultipart(path, operationProperties("delete")) {
            asObjectFromJson(it, RepositoryResult::class.java)
        }
    } catch (e: RequestException) {
        throw RepositoryException("Cannot delete repository node: $path", e)
    }

    fun reload() {
        reloadProperties()
    }

    fun saveProperty(name: String, value: Any?): RepositoryResult = save(mapOf(name to value))

    fun deleteProperty(name: String): RepositoryResult = saveProperty(name, null)

    fun hasProperty(name: String): Boolean = properties.containsKey(name)

    private fun reloadProperties(): Properties = repository.http.get("$path.json") {
        Properties(asJson(it).json<LinkedHashMap<String, Any>>()).apply { propertiesLoaded = this }
    }

    /**
     * Implementation for supporting "Controlling Content Updates with @ Suffixes"
     * @see <https://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html>
     */
    private fun postProperties(properties: Map<String, Any?>, removeOther: Boolean): Map<String, Any?> {
        var result = properties.entries.fold(mutableMapOf<String, Any?>(), { props, (name, value) ->
            when {
                value == null && repository.nullDeletes -> props["$name@Delete"] = ""
                else -> {
                    props[name] = value
                    if (repository.typeHints) {
                        TypeHint.of(value)?.let { props["$name@TypeHint"] = it }
                    }
                }
            }
            props
        })

        if (removeOther) {
            result = mutableMapOf<String, Any?>().apply {
                putAll(result)
                this@Node.properties.keys.filter { !result.keys.contains(it) }.forEach { put("$it@Delete", "") }

            }
        }

        return result
    }

    private fun operationProperties(operation: String): Map<String, Any?> = mapOf(
            ":operation" to operation,
            ":http-equiv-accept" to "application/json"
    )

    private fun filterMetaProperties(properties: Map<String, Any>): Map<String, Any> {
        return properties.filterKeys { p -> !Property.values().any { it.value == p } }
    }

    fun recurse(self: Boolean = false): Sequence<Node> = sequence {
        val stack = Stack<Node>()

        if (self) {
            stack.add(this@Node)
        } else {
            stack.addAll(children)
        }

        while (stack.isNotEmpty()) {
            val current = stack.pop()
            stack.addAll(current.children)
            yield(current)
        }
    }

    @get:JsonIgnore
    val json: String
        get() = Formats.toJson(this)

    override fun toString(): String {
        return "Node(path='$path', type='$type', properties=$properties)"
    }

    enum class Property(val value: String) {
        CHILDREN("__children__"),
        NAME("__name__")
    }
}
