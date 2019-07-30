package com.cognifide.gradle.aem.common.instance.service.repository

import com.cognifide.gradle.aem.common.http.RequestException
import com.cognifide.gradle.aem.common.utils.Formats
import com.fasterxml.jackson.annotation.JsonIgnore
import com.jayway.jsonpath.PathNotFoundException
import java.io.Serializable
import java.util.*
import net.minidev.json.JSONArray
import org.apache.jackrabbit.vault.util.JcrConstants

/**
 * Represents node stored in JCR content repository.
 */
class Node(private val repository: Repository, val path: String) : Serializable {

    private val logger = repository.aem.logger

    /**
     * Cached properties of node.
     */
    private var propertiesLoaded: Properties? = null

    /**
     * Node name
     */
    val name: String
        get() = path.substringAfterLast("/")

    /**
     * JCR node properties.
     *
     * Keep in mind that these values are loaded lazily and sometimes it is needed to reload them
     * using dedicated method.
     */
    val properties: Properties
        get() = propertiesLoaded ?: reloadProperties()

    /**
     * JCR primary type of node.
     */
    @get:JsonIgnore
    val type: String
        get() = properties.string(JcrConstants.JCR_PRIMARYTYPE)!!

    /**
     * Parent node.
     */
    @get:JsonIgnore
    val parent: Node
        get() = Node(repository, path.substringBeforeLast("/"))

    /**
     * Get all node child nodes.
     *
     * Because of performance issues, using method is more preferred.
     */
    @get:JsonIgnore
    val children: List<Node>
        get() = children().toList()

    /**
     * Loop over all node child nodes.
     */
    @Suppress("unchecked_cast")
    fun children(): Sequence<Node> {
        logger.info("Reading child nodes of repository node '$path'")

        return try {
            repository.http.get("$path.harray.1.json") { asJson(it) }
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
                        propertiesLoaded = Properties(this, filterMetaProperties(props))
                    }
                }
                .asSequence()
        } catch (e: RequestException) {
            throw RepositoryException("Cannot read children of node: $path. Cause: ${e.message}", e)
        }
    }

    /**
     * Create or update node in repository.
     */
    fun save(properties: Map<String, Any?>): RepositoryResult = try {
        logger.info("Saving repository node '$path', properties '$properties'")

        repository.http.postMultipart(path, postProperties(properties) + operationProperties("")) {
            asObjectFromJson(it, RepositoryResult::class.java)
        }
    } catch (e: RequestException) {
        throw RepositoryException("Cannot save repository node: $path. Cause: ${e.message}", e)
    }

    /**
     * Delete node and all children from repository.
     */
    fun delete(): RepositoryResult = try {
        logger.info("Deleting repository node '$path'")

        repository.http.postMultipart(path, operationProperties("delete")) {
            asObjectFromJson(it, RepositoryResult::class.java)
        }
    } catch (e: RequestException) {
        throw RepositoryException("Cannot delete repository node: $path. Cause: ${e.message}", e)
    }

    /**
     * Deletes node and creates it again. Use with caution!
     */
    fun replace(properties: Map<String, Any?>): RepositoryResult {
        delete()
        return save(properties)
    }

    /**
     * Synchronizes on demand previously loaded properties of node (by default properties are loaded lazily).
     * Useful when saving and working on same node again (without instantiating variable).
     */
    fun reload() {
        reloadProperties()
    }

    /**
     * Search nodes by traversing a node tree.
     * Use sequence filter method to find desired nodes.
     */
    fun traverse(self: Boolean = false): Sequence<Node> = sequence {
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

    /**
     * Update only single property of node.
     */
    fun saveProperty(name: String, value: Any?): RepositoryResult = save(mapOf(name to value))

    /**
     * Delete single property from node.
     */
    fun deleteProperty(name: String): RepositoryResult = deleteProperties(listOf(name))

    /**
     * Delete multiple properties from node.
     */
    fun deleteProperties(vararg names: String) = deleteProperties(names.asIterable())

    /**
     * Delete multiple properties from node.
     */
    fun deleteProperties(names: Iterable<String>): RepositoryResult {
        return save(names.fold(mutableMapOf()) { props, name -> props[name] = null; props })
    }

    /**
     * Check if node has property.
     */
    fun hasProperty(name: String): Boolean = properties.containsKey(name)

    /**
     * Check if node has properties.
     */
    fun hasProperties(vararg names: String) = hasProperties(names.asIterable())

    /**
     * Check if node has properties.
     */
    fun hasProperties(names: Iterable<String>): Boolean = names.all { properties.containsKey(it) }

    private fun reloadProperties(): Properties {
        logger.info("Reading properties of repository node '$path'")

        return try {
            repository.http.get("$path.json") {
                Properties(this@Node, asJson(it).json<LinkedHashMap<String, Any>>()).apply { propertiesLoaded = this }
            }
        } catch (e: RequestException) {
            throw RepositoryException("Cannot read properties of node: $path. Cause: ${e.message}", e)
        }
    }

    /**
     * Implementation for supporting "Controlling Content Updates with @ Suffixes"
     * @see <https://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html>
     */
    private fun postProperties(properties: Map<String, Any?>): Map<String, Any?> {
        return properties.entries.fold(mutableMapOf()) { props, (name, value) ->
            when (value) {
                null -> props["$name@Delete"] = ""
                else -> {
                    props[name] = RepositoryType.normalize(value)
                    if (repository.typeHints) {
                        RepositoryType.hint(value)?.let { props["$name@TypeHint"] = it }
                    }
                }
            }
            props
        }
    }

    private fun operationProperties(operation: String): Map<String, Any?> = mapOf(
            ":operation" to operation,
            ":http-equiv-accept" to "application/json"
    )

    private fun filterMetaProperties(properties: Map<String, Any>): Map<String, Any> {
        return properties.filterKeys { p -> !Property.values().any { it.value == p } }
    }

    @get:JsonIgnore
    val json: String
        get() = Formats.toJson(this)

    override fun toString(): String {
        return "Node(path='$path', properties=$properties)"
    }

    enum class Property(val value: String) {
        CHILDREN("__children__"),
        NAME("__name__")
    }
}
