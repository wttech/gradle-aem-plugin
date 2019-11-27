package com.cognifide.gradle.aem.test

import com.cognifide.gradle.aem.common.utils.Patterns
import com.cognifide.gradle.aem.instance.tail.*
import org.apache.commons.io.FileUtils
import org.gradle.util.GFileUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.time.ZoneId
import java.util.*

class InstanceTailTest {

    class MockSource(vararg resources: String) : LogSource {

        private val streamsStack = Stack<BufferedReader>().apply {
            addAll(resources.reversed().map { reader(it) })
        }

        override fun <T> readChunk(parser: (BufferedReader) -> List<T>): List<T> = streamsStack.pop().use(parser)

        companion object {
            fun reader(resource: String) = BufferedReader(InputStreamReader(GFileUtils.openInputStream(file(resource))))

            fun text(resource: String): String = FileUtils.readFileToString(file(resource), "UTF8")

            private fun file(resource: String) = File(this::class.java.classLoader.getResource(resource)!!.file)
        }

    }

    class MockDestination : LogDestination {
        val logsList = mutableListOf<Log>()

        override fun dump(logs: List<Log>) {
            logsList += logs
        }
    }

    @Test
    fun shouldParseLogs() {
        // given
        val parser = LogParser(DEFAULT_ZONE_ID)
        val logsChunk = MockSource.reader("com/cognifide/gradle/aem/test/instance-tail/10-logs-error.log")

        // when
        val logsList = parser.parse(logsChunk)

        // then
        assertEquals(10, logsList.size)

        logsList.first().apply {
            assertEquals(Log.parseTimestamp("14.01.2019 12:19:48.350", DEFAULT_ZONE_ID), timestamp)
            assertEquals("INFO", level)
            assertEquals("[0:0:0:0:0:0:0:1 [1547464785823] GET /rge.etkrggtkgk/etkrggtkgk/gaegkgr/ragrae/gaegkgr.gkg.rk HTTP/1.1]", source)
            assertEquals("egg.erggr.gaegkgr.gk.etkrggtkgk.kgrt.HggtLkgaeaeMegegraIgrt Sgeag ggktrkgg JS tkgaeae: /tkgk/etkrggtkgk/gaegkgr/ragrae/gaegkgr (gkgkpkrr)", message)
            assertEquals("c298a80e1b5083dea0c9dbf12b045a67", checksum)
        }
        logsList.last().apply {
            assertEquals(Log.parseTimestamp("14.01.2019 12:20:43.111", DEFAULT_ZONE_ID), timestamp)
            assertEquals("ERROR", level)
            assertEquals("[gea-arrgkkggae-rtreggga-1] egg.erggr.gaegkgr.arrgkkggae Sragker [6848, [gag.ereeer.reeaaeggkg.gea.erk.rgt.SrkkkggMBreg]]", source)
            assertEquals("SragkerEgrgg REGISTERED", message)
            assertEquals("ddb1f42608d7c1fc7014cdbdb6a69f80", checksum)
        }
    }

    @Test
    fun shouldParseMultilineLogs() {
        // given
        val parser = LogParser(DEFAULT_ZONE_ID)
        val logsChunk = MockSource.reader("com/cognifide/gradle/aem/test/instance-tail/aggregating/multiline/multiline-logs-error.log")

        // when
        val logsList = parser.parse(logsChunk)

        // then
        assertEquals(4, logsList.size)

        logsList[0].apply {
            assertEquals(Log.parseTimestamp("14.01.2019 12:20:05.242", DEFAULT_ZONE_ID), timestamp)
            assertEquals("WARN", level)
            assertEquals("[0:0:0:0:0:0:0:1 [1547464792884] GET /llr.resllleskr/resllleskr/rcslsll/rrcsess3.fsl.cr HTTP/1.1]", source)
            assertEqualsIgnoringCr(MockSource.text("com/cognifide/gradle/aem/test/instance-tail/aggregating/multiline/multiline-short.log"), message)
            assertEquals("148a7ab608478f4a609d428a28773fc8", checksum)
        }
        logsList[2].apply {
            assertEquals(Log.parseTimestamp("14.01.2019 12:04:58.535", DEFAULT_ZONE_ID), timestamp)
            assertEquals("WARN", level)
            assertEquals("[reslr-rsf-rkrlcsslsrl-2]", source)
            assertEqualsIgnoringCr(MockSource.text("com/cognifide/gradle/aem/test/instance-tail/aggregating/multiline/multiline-long.log"), message)
            assertEquals("46f3b3368c92b1e7400643ab8b1f3e3a", checksum)
        }
    }

    @Test
    fun shouldSkipIncompleteMultilineLogs() {
        // given
        val parser = LogParser(DEFAULT_ZONE_ID)
        val logsChunk = MockSource.reader("com/cognifide/gradle/aem/test/instance-tail/aggregating/multiline/incomplete-multiline-logs-error.log")

        // when
        val logsList = parser.parse(logsChunk)

        // then
        assertEquals(3, logsList.size)

        logsList.first().apply {
            assertEquals(Log.parseTimestamp("14.01.2019 12:20:05.242", DEFAULT_ZONE_ID), timestamp)
            assertEquals("6fe84dd875d8ca95b4f061a57b3c815d", checksum)
        }
    }

    @Test
    fun shouldAggregateOverlappingLogsBetweenRequests() {
        // given
        val source = MockSource(
            "com/cognifide/gradle/aem/test/instance-tail/aggregating/overlapping/first-chunk-error.log",
            "com/cognifide/gradle/aem/test/instance-tail/aggregating/overlapping/second-chunk-error.log")
        val destination = MockDestination()
        val tailer = LogTailer(source, destination, DEFAULT_ZONE_ID)

        // when
        tailer.tail()
        tailer.tail()

        // then
        val logs = destination.logsList
        assertEquals(11, logs.size)

        logs.first().apply {
            assertEquals(Log.parseTimestamp("14.01.2019 12:04:54.613", DEFAULT_ZONE_ID), timestamp)
            assertEquals("d37f9ce5287800493de0b6ef5bb43338", checksum)
        }
        logs.last().apply {
            assertEquals(Log.parseTimestamp("14.01.2019 12:04:58.773", DEFAULT_ZONE_ID), timestamp)
            assertEquals("68b331e94d5fe5ec182207198b149535", checksum)
        }
    }

    @Test
    fun shouldAggregateWhenThereIsNoOverlapping() {
        // given
        val source = MockSource(
            "com/cognifide/gradle/aem/test/instance-tail/aggregating/disjoint/first-chunk-error.log",
            "com/cognifide/gradle/aem/test/instance-tail/aggregating/disjoint/second-chunk-error.log")
        val destination = MockDestination()
        val tailer = LogTailer(source, destination, DEFAULT_ZONE_ID)

        // when
        tailer.tail()
        tailer.tail()

        // then
        val logs = destination.logsList
        assertEquals(10, logs.size)

        logs.first().apply {
            assertEquals(Log.parseTimestamp("14.01.2019 12:04:54.613", DEFAULT_ZONE_ID), timestamp)
            assertEquals("1310668b557d5e87686385dfd8c82bdb", checksum)
        }
        logs.last().apply {
            assertEquals(Log.parseTimestamp("14.01.2019 12:04:58.773", DEFAULT_ZONE_ID), timestamp)
            assertEquals("7071e8cc64b8d3821dea686b20ba5b58", checksum)
        }
    }

    @Test
    fun shouldSkipLogsChunkWhenThereAreNoNewLogs() {
        // given
        val source = MockSource(
            "com/cognifide/gradle/aem/test/instance-tail/aggregating/disjoint/first-chunk-error.log",
            "com/cognifide/gradle/aem/test/instance-tail/aggregating/disjoint/first-chunk-error.log")
        val destination = MockDestination()
        val tailer = LogTailer(source, destination, DEFAULT_ZONE_ID)

        // when
        tailer.tail()
        tailer.tail()

        // then
        val logs = destination.logsList
        assertEquals(5, logs.size)

        logs.first().apply {
            assertEquals(Log.parseTimestamp("14.01.2019 12:04:54.613", DEFAULT_ZONE_ID), timestamp)
            assertEquals("1310668b557d5e87686385dfd8c82bdb", checksum)
        }
        logs.last().apply {
            assertEquals(Log.parseTimestamp("14.01.2019 12:04:58.519", DEFAULT_ZONE_ID), timestamp)
            assertEquals("b410a72d5bc75b608c2c6f0014f9d88b", checksum)
        }
    }

    @Test
    fun shouldFilterOutAllErrorBasedOnLambdas() {
        //given
        val filter: (Log) -> Boolean = { log -> log.message.isNotEmpty() }

        //when
        val logFilter = LogFilter().apply { excludeRule(filter) }

        //then
        assertTrue(logFilter.isExcluded(Log.create(listOf("14.01.2019 12:20:43.111 *ERROR* " +
            "[gea-arrgkkggae-rtreggga-1] egg.erggr.gaegkgr.arrgkkggae Sragker " +
            "[6848, [gag.ereeer.reeaaeggkg.gea.erk.rgt.SrkkkggMBreg]] SragkerEgrgg REGISTERED"), DEFAULT_ZONE_ID)))
    }

    @Test
    fun shouldLetGoAllErrorBasedOnLambdas() {
        //given
        val filter: (Log) -> Boolean = { log -> log.message.isEmpty() }

        //when
        val blacklist = LogFilter().apply { excludeRule(filter) }

        //then
        assertFalse(blacklist.isExcluded(Log.create(listOf("14.01.2019 12:20:43.111 *ERROR* " +
            "[gea-arrgkkggae-rtreggga-1] egg.erggr.gaegkgr.arrgkkggae Sragker " +
            "[6848, [gag.ereeer.reeaaeggkg.gea.erk.rgt.SrkkkggMBreg]] SragkerEgrgg REGISTERED"), DEFAULT_ZONE_ID)))
    }

    @Test
    fun shouldFilterOutAllErrorBasedOnWildcard() {
        //given
        val filter: (Log) -> Boolean = { Patterns.wildcard(it.text, "*egg.erggr.gaegkgr.*") }

        //when
        val logFilter = LogFilter().apply { excludeRule(filter) }

        //then
        assertTrue(logFilter.isExcluded(Log.create(listOf("14.01.2019 12:20:43.111 *ERROR* " +
            "[gea-arrgkkggae-rtreggga-1] egg.erggr.gaegkgr.arrgkkggae Sragker " +
            "[6848, [gag.ereeer.reeaaeggkg.gea.erk.rgt.SrkkkggMBreg]] SragkerEgrgg REGISTERED"), DEFAULT_ZONE_ID)))
    }

    @Test
    fun shouldLetGoAllErrorBasedOnWildcard() {
        //given
        val filter: (Log) -> Boolean = { Patterns.wildcard(it.text, "a.b.cde.*") }

        //when
        val blacklist = LogFilter().apply { excludeRule(filter) }

        //then
        assertFalse(blacklist.isExcluded(Log.create(listOf("14.01.2019 12:20:43.111 *ERROR* " +
            "[gea-arrgkkggae-rtreggga-1] egg.erggr.gaegkgr.arrgkkggae Sragker " +
            "[6848, [gag.ereeer.reeaaeggkg.gea.erk.rgt.SrkkkggMBreg]] SragkerEgrgg REGISTERED"), DEFAULT_ZONE_ID)))
    }

    private fun assertEqualsIgnoringCr(expected: String, actual: String) {
        assertEquals(removeCr(expected), removeCr(actual))
    }

    private fun removeCr(expected: String) = expected.replace("\r", "")

    companion object {
        private val DEFAULT_ZONE_ID = ZoneId.systemDefault()
    }
}
