package com.cognifide.gradle.aem.common.instance.service.repository

open class QueryParams(private val enumerated: Boolean) {

    internal val params = mutableMapOf<String, String>()

    fun param(definer: QueryParams.() -> Unit) {
        val params = QueryParams(false).apply(definer).params
        val index = paramIndex

        if (enumerated) {
            this.params.putAll(params.mapKeys { "${index}_${it.key}" })
        } else {
            this.params.putAll(params)
        }
    }

    private val paramIndex: Int get() = (params.keys
            .filter { it.matches(Regex("^\\d+_\\w+$")) }
            .map { it.split("_")[0].toInt() }
            .max() ?: 0) + 1

    // Node type filtering params

    fun type(value: String) = param {
        params["type"] = value
    }

    fun file() = type("nt:file")

    fun page() = type("cq:Page")

    fun pageContent() = type("cq:PageContent")

    fun damAsset() = type("dam:Asset")

    // Specialized filtering params

    fun name(wildcardPattern: String) = param {
        params["nodename"] = wildcardPattern
    }

    fun path(value: String) = param {
        params["path"] = value
    }

    fun depth(value: Int) = param {
        params["p.nodedepth"] = value.toString()
    }

    fun fullText(value: String, relativePath: String? = null) = param {
        params["fulltext"] = value
        if (relativePath != null) {
            params["fulltext.relPath"] = relativePath
        }
    }

    fun tag(value: String, property: String? = null) = param {
        params["tagid"] = value
        if (property != null) {
            params["tagid.property"] = property
        }
    }

    fun contentTag(value: String) = tag(value, "jcr:content/cq:tags")

    // Custom property filtering params

    fun propertyWhere(name: String, value: String? = null, operation: String? = null) = param {
        params["property"] = name
        if (value != null) {
            params["property.value"] = value
        }
        if (operation != null) {
            params["property.operation"] = operation
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

    fun propertyContains(name: String, values: Iterable<String>, all: Boolean = true) = param {
        params["property"] = name
        if (all) {
            params["property.and"] = "true"
        }
        values.forEachIndexed { vi, value ->
            params["property.${vi + 1}_value"] = value
        }
    }

    fun propertyContains(name: String, vararg values: String, all: Boolean = true) = propertyContains(name, values.asIterable(), all)

    fun propertyContainsAny(name: String, values: Iterable<String>) = propertyContains(name, values, false)

    fun propertyContainsAny(name: String, vararg values: String) = propertyContains(name, values.asIterable(), false)


}