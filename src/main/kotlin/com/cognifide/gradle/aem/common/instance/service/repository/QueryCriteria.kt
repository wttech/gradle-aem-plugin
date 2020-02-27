package com.cognifide.gradle.aem.common.instance.service.repository

@Suppress("TooManyFunctions")
class QueryCriteria {

    // All params

    val params = mutableMapOf(
            "p.limit" to "100" // performance optimization for batch processing (default is 10)
    )

    // Custom params

    fun property(definition: (Int) -> Unit) {
        definition(propertyIndex)
    }

    private val propertyIndex: Int get() = (params.keys
            .filter { it.endsWith("_property") }
            .map { it.split("_")[0].toInt() }
            .max() ?: 0) + 1

    // Node type filtering params

    fun type(value: String) {
        params["type"] = value
    }

    fun file() = type("nt:file")

    fun page() = type("cq:Page")

    fun pageContent() = type("cq:PageContent")

    fun damAsset() = type("dam:Asset")

    // Specialized filtering params

    fun name(wildcardPattern: String) {
        params["nodename"] = wildcardPattern
    }

    fun path(value: String) {
        params["path"] = value
    }

    fun depth(value: Int) {
        params["p.nodedepth"] = value.toString()
    }

    fun fullText(value: String, relativePath: String? = null) {
        params["fulltext"] = value
        if (relativePath != null) {
            params["fulltext.relPath"] = relativePath
        }
    }

    fun tag(value: String, property: String? = null) {
        params["tagid"] = value
        if (property != null) {
            params["tagid.property"] = property
        }
    }

    fun contentTag(value: String) = tag(value, "jcr:content/cq:tags")

    // Custom property filtering params

    fun propertyWhere(name: String, value: String? = null, operation: String? = null) = property { p ->
        params["${p}_property"] = name
        if (value != null) {
            params["${p}_property.value"] = value
        }
        if (operation != null) {
            params["${p}_property.operation"] = operation
        }
    }

    fun property(name: String, value: String) = propertyWhere(name, value)

    fun propertyLike(name: String, value: String) = propertyWhere(name, value, "like")

    fun propertyExists(name: String) = propertyWhere(name, true.toString(), "exists")

    fun propertyNotExists(name: String) = propertyWhere(name, false.toString(), "exists")

    fun propertyNot(name: String) = propertyWhere(name, null, "not")

    fun propertyEquals(name: String, value: String, ignoreCase: Boolean = false) = when (ignoreCase) {
        true -> propertyWhere(name, value, "equalsIgnoreCase")
        false -> propertyWhere(name, value, "equals")
    }

    fun propertyNotEquals(name: String, value: String, ignoreCase: Boolean = false) = when (ignoreCase) {
        true -> propertyWhere(name, value, "unequalsIgnoreCase")
        false -> propertyWhere(name, value, "unequals")
    }

    fun propertyContains(name: String, values: Iterable<String>, all: Boolean = true) = property { pi ->
        params["${pi}_property"] = name
        if (all) {
            params["${pi}_property.and"] = "true"
        }
        values.forEachIndexed { vi, value ->
            params["${pi}_property.${vi + 1}_value"] = value
        }
    }

    fun propertyContains(name: String, vararg values: String, all: Boolean = true) = propertyContains(name, values.asIterable(), all)

    fun propertyContainsAny(name: String, values: Iterable<String>) = propertyContains(name, values, false)

    fun propertyContainsAny(name: String, vararg values: String) = propertyContains(name, values.asIterable(), false)

    // Ordering params

    fun orderBy(value: String, sort: String = "asc") {
        params["orderby"] = value
        if (sort != "asc") {
            params["orderby.sort"] = sort
        }
    }

    fun orderByPath(sort: String = "asc") = orderBy("path", sort)

    fun orderByName(sort: String = "asc") = orderBy("nodename", sort)

    fun orderByProperty(name: String, sort: String = "asc") = orderBy("@$name", sort)

    fun orderByScore() = orderByProperty("jcr:score", "desc")

    fun orderByLastModified(sort: String = "desc") = orderByProperty("cq:lastModified", sort)

    fun orderByContentLastModified(sort: String = "desc") = orderByProperty("jcr:content/cq:lastModified", sort)

    fun offset(value: Int) {
        params["p.offset"] = value.toString()
    }

    // Paginating params

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

    companion object {
        private val FORCED_PARAMS = mapOf(
                "p.guessTotal" to "true",
                "p.hits" to "full"
        )
    }
}
