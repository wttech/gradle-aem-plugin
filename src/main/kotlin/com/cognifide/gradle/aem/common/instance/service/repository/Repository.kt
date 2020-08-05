package com.cognifide.gradle.aem.common.instance.service.repository

import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.common.http.RequestException
import com.cognifide.gradle.common.http.ResponseException
import java.io.File

class Repository(sync: InstanceSync) : InstanceService(sync) {

    /**
     * Take care about property value types saved in repository.
     */
    val typeHints = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("instance.repository.typeHints")?.let { set(it) }
    }

    /**
     * Controls level of logging. By default repository related operations are only logged at debug level.
     * This switch could increase logging level to info level.
     */
    val verboseLogging = aem.obj.boolean {
        convention(false)
        aem.prop.boolean("instance.repository.verboseLogging")?.let { set(it) }
    }

    /**
     * Controls throwing exceptions in case of response statuses indicating repository errors.
     * Switching it to false, allows custom error handling in task scripting.
     */
    val responseChecks = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("instance.repository.responseChecks")?.let { set(it) }
    }

    /**
     * When trying to upload file under '/content/dam', repository will use for upload dedicated AEM service
     * instead of using Sling service.
     */
    val damUploads = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("instance.repository.damUploads")?.let { set(it) }
    }

    /**
     * Get node at given path.
     */
    fun node(path: String) = Node(this, path)

    /**
     * Get node at given path and perform action in its scope (and optionally return result).
     */
    fun <T> node(path: String, action: Node.() -> T): T = node(path).run(action)

     /**
     * Shorthand method for creating or updating node at given path.
     */
    fun save(path: String, properties: Map<String, Any?>): RepositoryResult {
         val (dir, name) = splitPath(path)
         return when {
             name.contains(".") -> node(dir).import(properties, name, replace = true, replaceProperties = true)
             else -> node(path).save(properties)
         }
    }

    /**
     * Shorthand method for importing content from JSON file at given path.
     */
    fun import(path: String, jsonFile: File): RepositoryResult {
        val (dir, name) = splitPath(path)
        return node(dir).import(jsonFile, name, replace = true, replaceProperties = true)
    }

    /**
     * Execute repository query to find desired nodes.
     */
    fun query(criteria: QueryCriteria.() -> Unit): Query = query(QueryCriteria().apply(criteria))

    /**
     * Execute repository query to find desired nodes.
     */
    fun query(criteria: QueryCriteria): Query = try {
        val path = "$QUERY_BUILDER_PATH?${criteria.queryString}"
        log("Querying repository using URL '${instance.httpUrl}$path'")
        val result = http.get(path) { asObjectFromJson<QueryResult>(it) }
        Query(this, criteria, result)
    } catch (e: RequestException) {
        throw RepositoryException("Cannot perform $criteria on $instance. Cause: ${e.message}", e)
    } catch (e: ResponseException) {
        throw RepositoryException("Malformed response after querying $criteria on $instance. Cause: ${e.message}", e)
    }

    fun replicationAgent(location: String, name: String) = ReplicationAgent(node("/etc/replication/agents.$location/$name"))

    fun replicationAgents(location: String): Sequence<ReplicationAgent> = node("/etc/replication/agents.$location").siblings()
            .filter { it.type == "cq:Page" }
            .map { ReplicationAgent(it) }

    fun replicationAgents(): Sequence<ReplicationAgent> {
        return replicationAgents(ReplicationAgent.LOCATION_AUTHOR) + replicationAgents(ReplicationAgent.LOCATION_PUBLISH)
    }

    val replicationAgents: List<ReplicationAgent> get() = replicationAgents().toList()

    private fun splitPath(path: String): Pair<String, String> {
        return path.substringBeforeLast("/") to path.substringAfterLast("/")
    }

    internal fun log(message: String, e: Throwable? = null) = when {
        verboseLogging.get() -> logger.info(message, e)
        else -> logger.debug(message, e)
    }

    internal val http by lazy {
        RepositoryHttpClient(aem, instance).apply {
            responseChecks.set(this@Repository.responseChecks)
        }
    }

    companion object {
        const val QUERY_BUILDER_PATH = "/bin/querybuilder.json"
    }
}
