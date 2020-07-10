package com.cognifide.gradle.sling.common.instance.service.repository

class Query(val repository: Repository, val criteria: QueryCriteria, val result: QueryResult) {

    val nodes: List<Node> get() = result.hits.map { Node(repository, it["jcr:path"] as String, it) }

    fun nodeSequence(): Sequence<Node> = sequence {
        yieldAll(nodes)

        if (result.more) {
            var criteriaMore = criteria.forMore()
            do {
                val query = repository.query(criteriaMore)
                yieldAll(query.nodes)
                criteriaMore = criteriaMore.forMore()
            } while (query.result.more)
        }
    }

    override fun toString(): String = "Query(criteria=$criteria, result=$result)"
}
