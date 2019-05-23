package com.cognifide.gradle.aem.instance.service.groovy

import com.cognifide.gradle.aem.common.AemException
import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.common.Patterns
import com.cognifide.gradle.aem.instance.InstanceException
import com.cognifide.gradle.aem.instance.InstanceService
import com.cognifide.gradle.aem.instance.InstanceSync
import java.io.File

/**
 * Allows to execute Groovy code / scripts on AEM instance having Groovy Console CRX package installed.
 *
 * @see <https://github.com/icfnext/aem-groovy-console>
 */
class GroovyConsole(sync: InstanceSync) : InstanceService(sync) {

    fun evalCode(code: String, data: Map<String, Any> = mapOf(), verbose: Boolean = true): GroovyConsoleResult {
        val result = try {
            aem.logger.info("Executing Groovy Code: $code")
            evalCodeInternal(code, data)
        } catch (e: AemException) {
            throw InstanceException("Cannot evaluate Groovy code properly on $instance, code:\n$code", e)
        }

        if (verbose && result.exceptionStackTrace.isNotBlank()) {
            aem.logger.debug(result.toString())
            throw InstanceException("Execution of Groovy code on $instance ended with exception:\n${result.exceptionStackTrace}")
        }

        return result
    }

    private fun evalCodeInternal(code: String, data: Map<String, Any>): GroovyConsoleResult {
        return sync.postMultipart(EVAL_PATH, mapOf(
                "script" to code,
                "data" to Formats.toJson(data)
        )) { asObjectFromJson(it, GroovyConsoleResult::class.java) }
    }

    fun evalGroovyScript(file: File, data: Map<String, Any> = mapOf(), verbose: Boolean = true): GroovyConsoleResult {
        val result = try {
            aem.logger.info("Executing Groovy script: $file")
            evalCodeInternal(file.bufferedReader().use { it.readText() }, data)
        } catch (e: AemException) {
            throw InstanceException("Cannot evaluate Groovy script properly on $instance, file: $file", e)
        }

        if (verbose && result.exceptionStackTrace.isNotBlank()) {
            aem.logger.debug(result.toString())
            throw InstanceException("Execution of Groovy script $file on $instance ended with exception:\n${result.exceptionStackTrace}")
        }

        return result
    }

    fun evalScript(fileName: String, data: Map<String, Any> = mapOf(), verbose: Boolean = true): GroovyConsoleResult {
        val script = File(aem.groovyScriptRootDir, fileName)
        if (!script.exists()) {
            throw AemException("Groovy script '$fileName' not found in directory: ${aem.groovyScriptRootDir}")
        }

        return evalGroovyScript(script, data, verbose)
    }

    fun evalScripts(fileNamePattern: String = "**/*.groovy", data: Map<String, Any> = mapOf(), verbose: Boolean = true): Sequence<GroovyConsoleResult> {
        val scripts = (aem.groovyScriptRootDir.listFiles() ?: arrayOf()).filter {
            Patterns.wildcard(it, fileNamePattern)
        }.sortedBy { it.absolutePath }
        if (scripts.isEmpty()) {
            throw AemException("No Groovy scripts found in directory: ${aem.groovyScriptRootDir}")
        }

        return evalScripts(scripts, data, verbose)
    }

    fun evalScripts(scripts: Collection<File>, data: Map<String, Any> = mapOf(), verbose: Boolean = true): Sequence<GroovyConsoleResult> {
        return scripts.asSequence().map { evalGroovyScript(it, data, verbose) }
    }

    companion object {
        const val EVAL_PATH = "/bin/groovyconsole/post.json"
    }
}