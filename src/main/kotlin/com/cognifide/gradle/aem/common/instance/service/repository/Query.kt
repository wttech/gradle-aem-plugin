package com.cognifide.gradle.aem.common.instance.service.repository

class Query(val repository: Repository, val criteria: QueryCriteria, val result: QueryResult) {

    val nodes: List<Node> get() = result.hits.map { Node(repository, it["jcr:path"] as String, it) }

    override fun toString(): String = "Query(criteria=$criteria, result=$result)"
}
