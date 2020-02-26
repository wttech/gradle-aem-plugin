package com.cognifide.gradle.aem.common.instance.service.repository

import com.cognifide.gradle.aem.common.pkg.PackageDefinition
import com.cognifide.gradle.aem.common.utils.JcrUtil
import com.cognifide.gradle.common.CommonException
import com.cognifide.gradle.common.utils.Formats
import com.fasterxml.jackson.annotation.JsonIgnore
import com.jayway.jsonpath.PathNotFoundException
import net.minidev.json.JSONArray
import org.apache.commons.io.FilenameUtils
import org.apache.http.HttpStatus
import org.apache.jackrabbit.vault.util.JcrConstants
import java.io.File
import java.io.InputStream
import java.io.Serializable
import java.util.*

/**
 * Represents node stored in JCR content repository.
 */
@Suppress("TooManyFunctions")
class Node(val repository: Repository, val path: String) : Serializable {

    private val logger = repository.aem.logger

    private val instance = repository.instance

    private val http = repository.http

    /**
     * Cached properties of node.
     */
    private var propertiesLoaded: Properties? = null

    /**
     * Cached node existence check result.
     */
    private var existsCheck: Boolean? = null

    /**
     * Node name
     */
    val name: String get() = path.substringAfterLast("/")

    /**
     * File node base name.
     */
    @get:JsonIgnore
    val baseName: String get() = FilenameUtils.getBaseName(name)

    /**
     * File node extension.
     */
    @get:JsonIgnore
    val extension: String get() = FilenameUtils.getExtension(name)

    /**
     * JCR node properties.
     *
     * Keep in mind that these values are loaded lazily and sometimes it is needed to reload them
     * using dedicated method.
     */
    val properties: Properties get() = propertiesLoaded ?: reloadProperties()

    /**
     * JCR primary type of node.
     */
    @get:JsonIgnore
    val type: String get() = properties.string(JcrConstants.JCR_PRIMARYTYPE)!!

    /**
     * Parent node.
     */
    @get:JsonIgnore
    val parent: Node get() = Node(repository, path.substringBeforeLast("/"))

    /**
     * Get child node by name.
     */
    fun child(name: String) = Node(repository, "$path/$name")

    /**
     * Get all node child nodes.
     *
     * Because of performance issues, using method is more preferred.
     */
    @get:JsonIgnore
    val children: List<Node> get() = children().toList()

    /**
     * Loop over all node child nodes.
     */
    @Suppress("unchecked_cast")
    fun children(): Sequence<Node> {
        log("Reading child nodes of repository node '$path' on $instance")

        return try {
            http.get("$path.harray.1.json") { asJson(it) }
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
        } catch (e: CommonException) {
            throw RepositoryException("Cannot read children of node '$path' on $instance. Cause: ${e.message}", e)
        }
    }

    @get:JsonIgnore
    val siblings: List<Node> get() = siblings().toList()

    fun siblings() = parent.children().filter { it != this }

    fun sibling(name: String) = parent.child(name)

    /**
     * Check if node exists.
     *
     * Not checks again if properties of node are already loaded (skips extra HTTP request / optimization).
     */
    val exists: Boolean get() = propertiesLoaded != null || exists()

    /**
     * Check if node exists.
     */
    fun exists(recheck: Boolean = false): Boolean {
        if (recheck || existsCheck == null) {
            existsCheck = try {
                log("Checking repository node '$path' existence on $instance")
                http.head(path) { it.statusLine.statusCode != HttpStatus.SC_NOT_FOUND }
            } catch (e: CommonException) {
                throw RepositoryException("Cannot check repository node existence: $path on $instance. Cause: ${e.message}", e)
            }
        }

        return existsCheck!!
    }

    /**
     * Create or update node in repository.
     */
    fun save(properties: Map<String, Any?>): RepositoryResult = try {
        log("Saving repository node '$path' using properties '$properties' on $instance")

        http.postMultipart(path, postProperties(properties) + operationProperties("")) {
            asObjectFromJson(it, RepositoryResult::class.java)
        }
    } catch (e: CommonException) {
        throw RepositoryException("Cannot save repository node '$path' on $instance. Cause: ${e.message}", e)
    }

    /**
     * Import node into repository.
     *
     * Effectively it is an alternative method for saving node supporting dots in node names.
     */
    fun import(
        properties: Map<String, Any?>,
        name: String? = null,
        replace: Boolean = false,
        replaceProperties: Boolean = false
    ) = importInternal(importParams(properties, null, name, replace, replaceProperties))

    /**
     * Import content structure defined in JSON file into repository.
     */
    fun import(
        file: File,
        name: String? = null,
        replace: Boolean = false,
        replaceProperties: Boolean = false
    ): RepositoryResult {
        if (!file.exists()) {
            throw RepositoryException("File containing JSON content for node import does not exist: $file!")
        }

        return importInternal(importParams(null, file, name, replace, replaceProperties))
    }

    private fun importInternal(params: Map<String, Any?>) = try {
        log("Importing node '$name' into repository node '$path' on $instance")

        http.postMultipart(path, params) {
            asObjectFromJson(it, RepositoryResult::class.java)
        }
    } catch (e: CommonException) {
        throw RepositoryException("Cannot import node '$name' into repository node '$path' on $instance. Cause: ${e.message}", e)
    }

    private fun importParams(
        properties: Map<String, Any?>? = null,
        file: File? = null,
        name: String? = null,
        replace: Boolean = false,
        replaceProperties: Boolean = false
    ): Map<String, Any?> {
        return mutableMapOf<String, Any?>().apply {
            putAll(operationProperties("import"))
            putAll(mapOf(
                    ":replace" to replace,
                    ":replaceProperties" to replaceProperties,
                    ":contentType" to "json"
            ))
            name?.let { put(":name", it) }
            properties?.let { put(":content", Formats.toJson(properties)) }
            file?.let { put(":contentFile", file) }
        }
    }

    /**
     * Delete node and all children from repository.
     */
    fun delete(): RepositoryResult = try {
        log("Deleting repository node '$path' on $instance")

        http.postMultipart(path, operationProperties("delete")) {
            asObjectFromJson(it, RepositoryResult::class.java)
        }
    } catch (e: CommonException) {
        throw RepositoryException("Cannot delete repository node '$path' on $instance. Cause: ${e.message}", e)
    }

    /**
     * Delete node and creates it again. Use with caution!
     */
    fun replace(properties: Map<String, Any?>): RepositoryResult {
        delete()
        return save(properties)
    }

    /**
     * Synchronize on demand previously loaded properties of node (by default properties are loaded lazily).
     * Useful when saving and working on same node again (without instantiating separate variable).
     */
    fun reload() {
        reloadProperties()
    }

    /**
     * Copy node to from source path to destination path.
     */
    fun copy(targetPath: String): Node = try {
        http.postUrlencoded(path, operationProperties("copy") + mapOf(
                ":dest" to targetPath
        )) { checkStatus(it, HttpStatus.SC_CREATED) }

        Node(repository, targetPath)
    } catch (e: CommonException) {
        throw RepositoryException("Cannot copy repository node from '$path' to '$targetPath' on $instance. Cause: '${e.message}'")
    }

    /**
     * Move node from source path to destination path.
     */
    fun move(targetPath: String, replace: Boolean = false): Node = try {
        http.postUrlencoded(path, operationProperties("move") + mapOf(
                ":dest" to targetPath,
                ":replace" to replace
        )) { checkStatus(it, listOf(HttpStatus.SC_CREATED, HttpStatus.SC_OK)) }

        Node(repository, targetPath)
    } catch (e: CommonException) {
        throw RepositoryException("Cannot move repository node from '$path' to '$targetPath' on $instance. Cause: '${e.message}'")
    }

    /**
     * Copy node to path pointing to folder with preserving original node name.
     */
    fun copyTo(targetDir: String) = copy("$targetDir/$name")

    /**
     * Copy node from other path to current path.
     */
    fun copyFrom(sourcePath: String) = Node(repository, sourcePath).copy(path)

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
     * Search nodes by querying repository under node path.
     */
    fun query(options: Query.() -> Unit = {}): Sequence<Node> = repository.query {
        path(this@Node.path)
        options()
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
        log("Reading properties of repository node '$path' on $instance")

        return try {
            http.get("$path.json") { response ->
                val props = asJson(response).json<LinkedHashMap<String, Any>>()
                Properties(this@Node, props).apply { propertiesLoaded = this }
            }
        } catch (e: CommonException) {
            throw RepositoryException("Cannot read properties of node '$path' on $instance. Cause: ${e.message}", e)
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
                    if (repository.typeHints.get()) {
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

    private fun log(message: String, e: Throwable? = null) = when {
        repository.verboseLogging.get() -> logger.info(message, e)
        else -> logger.debug(message, e)
    }

    @get:JsonIgnore
    val json: String get() = Formats.toJson(this)

    /**
     * Upload file to node.
     *
     * If node path points to DAM, separate / dedicated endpoint is used automatically,
     * so that metadata and renditions are generated immediately.
     */
    fun upload(file: File) = when {
        repository.damUploads.get() && path.startsWith("$DAM_PATH/") -> uploadDamAsset(file)
        else -> uploadFile(file)
    }

    /**
     * Upload asset using default Sling endpoint.
     */
    fun uploadFile(file: File) {
        log("Uploading file '$file' to repository node '$path' on $instance")

        return try {
            http.postMultipart(parent.path, mapOf(name to file))
        } catch (e: CommonException) {
            throw RepositoryException("Cannot upload file '$file' to node '$path' on $instance. Cause: ${e.message}", e)
        }
    }

    /**
     * Upload asset using dedicated DAM endpoint.
     */
    fun uploadDamAsset(file: File) {
        log("Uploading DAM asset '$file' to repository node '$path' on $instance")

        return try {
            http.postMultipart("${parent.path}$DAM_UPLOAD_SUFFIX", mapOf(
                    "file" to file,
                    "fileName" to name
            ))
        } catch (e: CommonException) {
            throw RepositoryException("Cannot upload DAM asset '$file' to node '$path' on $instance. Cause: ${e.message}", e)
        }
    }

    /**
     * Upload file to current folder node with preserving original file name.
     */
    fun uploadTo(file: File) = repository.node("$path/${file.name}").apply { upload(file) }

    /**
     * Read file stored in node.
     */
    fun <T> read(reader: (InputStream) -> T) = http.get(path) { reader(asStream(it)) }

    /**
     * Download file stored in node to specified local file.
     */
    fun download(targetFile: File) {
        read { input -> targetFile.outputStream().use { output -> input.copyTo(output) } }
    }

    /**
     * Download file stored in node to temporary directory with preserving file name.
     */
    fun download() = downloadTo(repository.aem.common.temporaryDir)

    /**
     * Download file stored in node to specified local directory with preserving file name.
     */
    fun downloadTo(targetDir: File) = targetDir.resolve(name).apply { download(this) }

    /**
     * Download node as CRX package.
     */
    fun downloadPackage(options: PackageDefinition.() -> Unit = {}) = repository.sync.packageManager.download {
        archiveBaseName.set(JcrUtil.manglePath(this@Node.name))
        filter(this@Node.path)
        options()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Node

        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }

    override fun toString(): String {
        return "Node(path='$path', properties=$properties)"
    }

    enum class Property(val value: String) {
        CHILDREN("__children__"),
        NAME("__name__")
    }

    companion object {
        val TYPE_UNSTRUCTURED = JcrConstants.JCR_PRIMARYTYPE to JcrConstants.NT_UNSTRUCTURED

        const val DAM_PATH = "/content/dam"

        const val DAM_UPLOAD_SUFFIX = ".createasset.html"
    }
}
