package com.cognifide.gradle.aem.common.instance.service.groovy

import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.common.CommonException
import com.cognifide.gradle.common.utils.Formats
import java.io.File

/**
 * Allows to execute Groovy code / scripts on AEM instance having Groovy Console CRX package installed.
 *
 * @see <https://github.com/icfnext/aem-groovy-console>
 */
class GroovyConsole(sync: InstanceSync) : InstanceService(sync) {

    /**
     * Controls throwing exception on script execution error.
     */
    val verbose = aem.obj.boolean {
        convention(aem.commonOptions.verbose)
        aem.prop.boolean("instance.groovyConsole.verbose")?.let { set(it) }
    }

    /**
     * Directory to search for scripts to be evaluated.
     */
    val scriptDir = aem.obj.dir {
        convention(aem.instanceManager.configDir.dir("groovyScript"))
        aem.prop.file("instance.groovyConsole.scriptDir")?.let { set(it) }
    }

    /**
     * Check if console is installed on instance.
     */
    val available: Boolean get() = sync.osgiFramework.findBundle(SYMBOLIC_NAME)?.stable ?: false

    /**
     * Ensure by throwing exception that console is available on instance.
     */
    fun requireAvailable() {
        if (!available) {
            throw GroovyConsoleException(
                "Groovy console is not available on $instance!\n" +
                    "Ensure having correct URLs defined & credentials, granted access and networking in correct state (internet accessible, VPN on/off)"
            )
        }
    }

    /**
     * Evaluate Groovy code snippet on AEM instance.
     */
    fun evalCode(code: String, data: Map<String, Any?> = mapOf()): GroovyEvalResult {
        val result = try {
            aem.logger.info("Evaluating Groovy Code: $code")
            evalCodeInternal(code, data)
        } catch (e: CommonException) {
            throw GroovyConsoleException("Cannot evaluate Groovy code properly on $instance, code:\n$code, cause: ${e.message}", e)
        }

        if (verbose.get() && result.exceptionStackTrace.isNotBlank()) {
            aem.logger.debug(result.toString())
            throw GroovyConsoleException("Evaluation of Groovy code on $instance ended with exception:\n${result.exceptionStackTrace}")
        }

        return result
    }

    private fun evalCodeInternal(code: String, data: Map<String, Any?>): GroovyEvalResult {
        return sync.http.postMultipart(
            EVAL_PATH,
            mapOf(
                "script" to code,
                "data" to Formats.toJson(data)
            )
        ) { asObjectFromJson(it, GroovyEvalResult::class.java) }
    }

    /**
     * Evaluate any Groovy script on AEM instance.
     */
    fun evalScript(file: File, data: Map<String, Any?> = mapOf()): GroovyEvalResult {
        val result = try {
            aem.logger.info("Evaluating Groovy script '$file' on $instance")
            evalCodeInternal(file.bufferedReader().use { it.readText() }, data)
        } catch (e: CommonException) {
            throw GroovyConsoleException("Cannot evaluate Groovy script '$file' properly on $instance. Cause: ${e.message}", e)
        }

        if (verbose.get() && result.exceptionStackTrace.isNotBlank()) {
            aem.logger.debug(result.toString())
            throw GroovyConsoleException("Evaluation of Groovy script '$file' on $instance ended with exception:\n${result.exceptionStackTrace}")
        }

        return result
    }

    /**
     * Evaluate Groovy script found by its file name on AEM instance.
     */
    fun evalScript(fileName: String, data: Map<String, Any?> = mapOf()): GroovyEvalResult {
        val script = scriptDir.get().asFile.resolve(fileName)
        if (!script.exists()) {
            throw GroovyConsoleException("Groovy script '$fileName' not found in directory: $scriptDir")
        }

        return evalScript(script, data)
    }

    /**
     * Find scripts matching file pattern in pre-configured directory.
     */
    fun findScripts(pathPattern: String): List<File> = project.fileTree(scriptDir)
        .matching { it.include(pathPattern) }
        .sortedBy { it.absolutePath }
        .toList()

    /**
     * Evaluate all Groovy scripts found by file name pattern on AEM instance in path-based alphabetical order.
     */
    fun evalScripts(
        pathPattern: String = "**/*.groovy",
        data: Map<String, Any> = mapOf(),
        resultConsumer: GroovyEvalResult.() -> Unit = {}
    ) {
        evalScripts(findScripts(pathPattern), data, resultConsumer)
    }

    /**
     * Evaluate any Groovy scripts on AEM instance in specified order.
     */
    fun evalScripts(
        scripts: Iterable<File>,
        data: Map<String, Any?> = mapOf(),
        resultConsumer: GroovyEvalResult.() -> Unit = {}
    ) {
        scripts.forEach { resultConsumer(evalScript(it, data)) }
    }

    companion object {
        const val EVAL_PATH = "/bin/groovyconsole/post.json"

        const val SYMBOLIC_NAME = "aem-groovy-console"
    }
}
