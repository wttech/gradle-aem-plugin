package com.cognifide.gradle.aem.common.instance.service.repository

@Suppress("TooManyFunctions")
class QueryCriteria : QueryParams(false) {

    // Grouped params (logic statements)

    fun or(paramsDefiner: QueryParams.() -> Unit) = group(true, paramsDefiner)

    fun and(paramsDefiner: QueryParams.() -> Unit) = group(false, paramsDefiner)

    private fun group(or: Boolean, paramsDefiner: QueryParams.() -> Unit) {
        val params = QueryParams(true).apply(paramsDefiner).params
        val index = groupIndex

        this.params["${index}_group.p.or"] = or.toString()
        this.params.putAll(params.mapKeys { "${index}_group.${it.key}" })
    }

    private val groupIndex: Int get() = (params.keys
            .filter { it.matches(Regex("^\\d+_group\\..*")) }
            .map { it.split("_")[0].toInt() }
            .max() ?: 0) + 1

    // Ordering params

    fun orderBy(value: String, desc: Boolean = false) {
        params["orderby"] = value
        if (desc) {
            params["orderby.sort"] = "desc"
        }
    }

    fun orderByPath(desc: Boolean = false) = orderBy("path", desc)

    fun orderByName(desc: Boolean = false) = orderBy("nodename", desc)

    fun orderByProperty(name: String, desc: Boolean = false) = orderBy("@$name", desc)

    fun orderByScore(desc: Boolean = true) = orderByProperty("jcr:score", desc)

    fun orderByLastModified(desc: Boolean = true) = orderByProperty("cq:lastModified", desc)

    fun orderByContentLastModified(desc: Boolean = true) = orderByProperty("jcr:content/cq:lastModified", desc)

    // Paginating params

    fun offset(value: Int) {
        params["p.offset"] = value.toString()
    }

    val offset: Int get() = params["p.offset"]?.toInt() ?: 0

    fun limit(value: Int) {
        params["p.limit"] = value.toString()
    }

    val limit: Int get() = params["p.limit"]?.toInt() ?: 10

    // Rest

    val queryString get() = (params + FORCED_PARAMS).entries
            .joinToString("&") { (k, v) -> "$k=$v" }

    fun copy() = QueryCriteria().apply { params.putAll(this@QueryCriteria.params) }

    fun forMore() = copy().apply {
        offset(offset + limit)
    }

    override fun toString(): String = "QueryCriteria($queryString)"

    init {
        limit(100) // performance improvement (default is 10)
    }

    companion object {
        private val FORCED_PARAMS = mapOf(
                "p.guessTotal" to "true",
                "p.hits" to "full"
        )
    }
}
