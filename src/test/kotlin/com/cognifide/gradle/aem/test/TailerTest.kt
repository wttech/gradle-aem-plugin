package com.cognifide.gradle.aem.test

import com.cognifide.gradle.aem.tooling.tail.*
import org.gradle.util.GFileUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.*


class TailerTest {

    class MockSource(vararg resources: String) : LogSource {

        private val streamsStack = Stack<BufferedReader>().apply {
            addAll(resources.reversed().map { MockSource.chunkReader(it) })
        }

        override fun nextReader(): BufferedReader = streamsStack.pop()

        companion object {
            fun chunkReader(resource: String) = BufferedReader(InputStreamReader(GFileUtils.openInputStream(File(this::class.java.classLoader.getResource(resource).file))))
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
        val parser = Parser()
        val logsChunk = MockSource.chunkReader("com/cognifide/gradle/aem/test/tail/10-logs-error.log")

        // when
        val logsList = parser.parseLogs(logsChunk)

        // then
        assertEquals(10, logsList.size)

        logsList.first().apply {
            assertEquals("14.01.2019 12:19:48.350", timestamp)
            assertEquals("INFO", level)
            assertEquals("[0:0:0:0:0:0:0:1 [1547464785823] GET /rge.etkrggtkgk/etkrggtkgk/gaegkgr/ragrae/gaegkgr.gkg.rk HTTP/1.1]", source)
            assertEquals("egg.erggr.gaegkgr.gk.etkrggtkgk.kgrt.HggtLkgaeaeMegegraIgrt Sgeag ggktrkgg JS tkgaeae: /tkgk/etkrggtkgk/gaegkgr/ragrae/gaegkgr (gkgkpkrr)", message)
            assertEquals("c298a80e1b5083dea0c9dbf12b045a67", checksum)
        }
        logsList.last().apply {
            assertEquals("14.01.2019 12:20:43.111", timestamp)
            assertEquals("ERROR", level)
            assertEquals("[gea-arrgkkggae-rtreggga-1] egg.erggr.gaegkgr.arrgkkggae Sragker [6848, [gag.ereeer.reeaaeggkg.gea.erk.rgt.SrkkkggMBreg]]", source)
            assertEquals("SragkerEgrgg REGISTERED", message)
            assertEquals("ddb1f42608d7c1fc7014cdbdb6a69f80", checksum)
        }
    }

    @Test
    fun shouldParseMultilineLogs() {
        // given
        val parser = Parser()
        val logsChunk = MockSource.chunkReader("com/cognifide/gradle/aem/test/tail/multiline-logs-error.log")

        // when
        val logsList = parser.parseLogs(logsChunk)

        // then
        assertEquals(4, logsList.size)

        logsList[0].apply {
            val multilineError = """
                rrf.rrrrel.csssrrcskl.crrrfk /eskr/resllleskr/rcslsll/rrcsess3.cr:69409: WARNING - slclsrcskel rrkl
                      cllscl lcsl;
                      ^^^^^^^^^^^^

                """.trimIndent()
            assertEquals("14.01.2019 12:20:05.242", timestamp)
            assertEquals("WARN", level)
            assertEquals("[0:0:0:0:0:0:0:1 [1547464792884] GET /llr.resllleskr/resllleskr/rcslsll/rrcsess3.fsl.cr HTTP/1.1]", source)
            assertEquals(multilineError, message)
            assertEquals("148a7ab608478f4a609d428a28773fc8", checksum)
        }
        logsList[2].apply {
            val multilineError = """
                rcr.sksrcl.csrfcskksl.rsf.crc.rlrrsrl.RlfclrcSlcsllrc Tcsr rlrrsrl csr klll skel frc 4079 fslsllr slk fsrcl kl rsl rf ksll. Crlrsklc srslr s fclrc rlrrsrl rc lskesrslec clfclrc lcl rlrrsrl.
                csss.eslr.Esrlklsrl: Tcl rlrrsrl ksr rclsllk clcl:
                        sl rcr.sksrcl.csrfcskksl.rsf.crc.rlrrsrl.RlfclrcSlcsllrc${'$'}LrrOlrl.<slsl>(RlfclrcSlcsllrc.csss:170) [rcr.sksrcl.csrfcskksl.rsf-crc:1.8.2]
                        sl rcr.sksrcl.csrfcskksl.rsf.crc.clkrrslrcc.RlkrrslrccIfke.errsl(RlkrrslrccIfke.csss:285) [rcr.sksrcl.csrfcskksl.rsf-crc:1.8.2]
                        sl rrf.skrkl.rcslsll.clkrrslrcc.sfke.CRX3RlkrrslrccIfke.errsl(CRX3RlkrrslrccIfke.csss:150) [rrf.skrkl.rcslsll.clkrrslrcc:1.4.88]
                        sl rrf.skrkl.rcslsll.clkrrslrcc.sfke.CRX3RlkrrslrccIfke.errsl(CRX3RlkrrslrccIfke.csss:241) [rrf.skrkl.rcslsll.clkrrslrcc:1.4.88]
                        sl rrf.skrkl.rcslsll.clkrrslrcc.sfke.SeslrRlkrrslrccIfke${'$'}4.csl(SeslrRlkrrslrccIfke.csss:177) [rrf.skrkl.rcslsll.clkrrslrcc:1.4.88]
                        sl rrf.skrkl.rcslsll.clkrrslrcc.sfke.SeslrRlkrrslrccIfke${'$'}4.csl(SeslrRlkrrslrccIfke.csss:174) [rrf.skrkl.rcslsll.clkrrslrcc:1.4.88]
                        sl csss.rlrscslc.ArrlrrCrllcreelc.krPcssselrlk(Nslssl Mllcrk)
                        sl cssss.rlrscslc.sslc.Sskclrl.krArPcssselrlk(Sskclrl.csss:549)
                        sl rrf.skrkl.rcslsll.clkrrslrcc.sfke.SeslrRlkrrslrccIfke.rclsllSlcssrlSlrrsrl(SeslrRlkrrslrccIfke.csss:174) [rrf.skrkl.rcslsll.clkrrslrcc:1.4.88]
                        sl rcr.sksrcl.reslr.crc.ksrl.AkrlcsrlSeslrRlkrrslrcc2.rclsllSlcssrlSlrrsrl(AkrlcsrlSeslrRlkrrslrcc2.csss:166) [rcr.sksrcl.reslr.crc.ksrl:3.0.4]
                        sl rcr.sksrcl.reslr.crc.ksrl.AkrlcsrlSeslrRlkrrslrcc2.errslSlcssrl(AkrlcsrlSeslrRlkrrslrcc2.csss:381) [rcr.sksrcl.reslr.crc.ksrl:3.0.4]
                        sl rcr.sksrcl.reslr.crc.clrrscrl.slllclse.cleklc.crc.JrcPcrssklcSlsllFsrlrcc.rclsllPcrssklcSlsll(JrcPcrssklcSlsllFsrlrcc.csss:116) [rcr.sksrcl.reslr.crc.clrrscrl:3.0.8]
                        sl rcr.sksrcl.reslr.crc.clrrscrl.slllclse.cleklc.crc.JrcRlrrscrlPcrssklc.sslclllsrsll(JrcRlrrscrlPcrssklc.csss:304) [rcr.sksrcl.reslr.crc.clrrscrl:3.0.8]
                        sl rcr.sksrcl.reslr.crc.clrrscrl.slllclse.cleklc.crc.JrcRlrrscrlPcrssklc.sslclllsrsll(JrcRlrrscrlPcrssklc.csss:76) [rcr.sksrcl.reslr.crc.clrrscrl:3.0.8]
                        sl rcr.sksrcl.reslr.clrrscrlclrreslc.sfke.kcrssklcr.rlsllfse.PcrssklcMslsrlc.sslclllsrsll(PcrssklcMslsrlc.csss:161) [rcr.sksrcl.reslr.clrrscrlclrreslc:1.5.34]
                        sl rcr.sksrcl.reslr.clrrscrlclrreslc.sfke.kcrssklcr.rlsllfse.PcrssklcMslsrlc.rllOcCclsllPcrssklc(PcrssklcMslsrlc.csss:87) [rcr.sksrcl.reslr.clrrscrlclrreslc:1.5.34]
                        sl rcr.sksrcl.reslr.clrrscrlclrreslc.sfke.kcrssklcr.rlsllfse.PcrssklcMslsrlc.sslclllsrsllAee(PcrssklcMslsrlc.csss:129) [rcr.sksrcl.reslr.clrrscrlclrreslc:1.5.34]
                        sl rcr.sksrcl.reslr.clrrscrlclrreslc.sfke.RlrrscrlRlrreslcIfke.rclsllCrllcre(RlrrscrlRlrreslcIfke.csss:138) [rcr.sksrcl.reslr.clrrscrlclrreslc:1.5.34]
                        sl rcr.sksrcl.reslr.clrrscrlclrreslc.sfke.RlrrscrlRlrreslcIfke.<slsl>(RlrrscrlRlrreslcIfke.csss:100) [rcr.sksrcl.reslr.clrrscrlclrreslc:1.5.34]
                        sl rcr.sksrcl.reslr.clrrscrlclrreslc.sfke.RlrrscrlRlrreslcIfke.<slsl>(RlrrscrlRlrreslcIfke.csss:94) [rcr.sksrcl.reslr.clrrscrlclrreslc:1.5.34]
                        sl rcr.sksrcl.reslr.clrrscrlclrreslc.sfke.CrffrlRlrrscrlRlrreslcFsrlrccIfke.rllRlrrscrlRlrreslcIlllclse(CrffrlRlrrscrlRlrreslcFsrlrccIfke.csss:263) [rcr.sksrcl.reslr.clrrscrlclrreslc:1.5.34]
                        sl rcr.sksrcl.reslr.clrrscrlclrreslc.sfke.CrffrlRlrrscrlRlrreslcFsrlrccIfke.rllSlcssrlRlrrscrlRlrreslc(CrffrlRlrrscrlRlrreslcFsrlrccIfke.csss:396) [rcr.sksrcl.reslr.clrrscrlclrreslc:1.5.34]
                        sl rcr.sksrcl.reslr.clrrscrlclrreslc.sfke.cleklc.RlrrscrlRlrreslcCrllcre.rllRlrrscrlTcklRlrrscrlRlrreslc(RlrrscrlRlrreslcCrllcre.csss:707) [rcr.sksrcl.reslr.clrrscrlclrreslc:1.5.34]
                        sl rcr.sksrcl.reslr.clrrscrlclrreslc.sfke.cleklc.RlrrscrlRlrreslcCrllcre.rllPsclllRlrrscrlTckl(RlrrscrlRlrreslcCrllcre.csss:731) [rcr.sksrcl.reslr.clrrscrlclrreslc:1.5.34]
                        sl rcr.sksrcl.reslr.clrrscrlclrreslc.sfke.RlrrscrlRlrreslcIfke.rllPsclllRlrrscrlTckl(RlrrscrlRlrreslcIfke.csss:1219) [rcr.sksrcl.reslr.clrrscrlclrreslc:1.5.34]
                        sl rcr.sksrcl.reslr.clrrscrlclrreslc.sfke.RlrrscrlRlrreslcIfke.rllPsclllRlrrscrlTckl(RlrrscrlRlrreslcIfke.csss:1208) [rcr.sksrcl.reslr.clrrscrlclrreslc:1.5.34]
                        sl rcr.sksrcl.reslr.clrrscrlclrreslc.sfke.RlrrscrlRlrreslcIfke.srRlrrscrlTckl(RlrrscrlRlrreslcIfke.csss:1236) [rcr.sksrcl.reslr.clrrscrlclrreslc:1.5.34]
                        sl rcr.sksrcl.reslr.sks.clrrscrl.AkrlcsrlRlrrscrl.srRlrrscrlTckl(AkrlcsrlRlrrscrl.csss:121) [rcr.sksrcl.reslr.sks:2.16.4]
                        sl rrf.skrkl.rc.krf.rlcel.slllclse.CrfkrllllSlcelIlfrCsrclIfke.rlEslll(CrfkrllllSlcelIlfrCsrclIfke.csss:273) [rrf.skrkl.rc.rrf.skrkl.rc.krf.rlcel:1.0.12]
                        sl rcr.sksrcl.csrfcskksl.rrffrlr.rkrlcsslsrl.LsrllllcTcsrflc${'$'}1.rlEslll(LsrllllcTcsrflc.csss:190) [rcr.sksrcl.csrfcskksl.csrfcskksl-crc-rrffrlr:2.16.0]
                        sl rcr.sksrcl.csrfcskksl.rsf.crc.rkrlcsslsrl.CcslrlPcrrlrrrc.rrlllllCcslrlk(CcslrlPcrrlrrrc.csss:508) [rcr.sksrcl.csrfcskksl.rsf-crc:1.8.2]
                        sl rcr.sksrcl.csrfcskksl.rsf.kesrslr.rkrlcsslsrl.FsellcslrDsrkslrclc.rrlllllCcslrlk(FsellcslrDsrkslrclc.csss:53) [rcr.sksrcl.csrfcskksl.rsf-rrcl:1.8.2]
                        sl rcr.sksrcl.csrfcskksl.rsf.rks.rrffsl.BsrfrcrslkOkrlcslc${'$'}1${'$'}1.rsee(BsrfrcrslkOkrlcslc.csss:128) [rcr.sksrcl.csrfcskksl.rsf-rlrcl-rks:1.8.2]
                        sl rcr.sksrcl.csrfcskksl.rsf.rks.rrffsl.BsrfrcrslkOkrlcslc${'$'}1${'$'}1.rsee(BsrfrcrslkOkrlcslc.csss:122) [rcr.sksrcl.csrfcskksl.rsf-rlrcl-rks:1.8.2]
                        sl csss.slse.rrlrscclll.FslsclTsrf.csl(FslsclTsrf.csss:266)
                        sl csss.slse.rrlrscclll.TcclskPrreEslrslrc.cslWrcflc(TcclskPrreEslrslrc.csss:1149)
                        sl csss.slse.rrlrscclll.TcclskPrreEslrslrc${'$'}Wrcflc.csl(TcclskPrreEslrslrc.csss:624)
                        sl csss.eslr.Tcclsk.csl(Tcclsk.csss:748)
                """.trimIndent()
            assertEquals("14.01.2019 12:04:58.535", timestamp)
            assertEquals("WARN", level)
            assertEquals("[reslr-rsf-rkrlcsslsrl-2]", source)
            assertEquals(multilineError, message)
            assertEquals("46f3b3368c92b1e7400643ab8b1f3e3a", checksum)
        }
    }

    @Test
    fun shouldSkipIncompleteMultilineLogs() {
        // given
        val parser = Parser()
        val logsChunk = MockSource.chunkReader("com/cognifide/gradle/aem/test/tail/incomplete-multiline-logs-error.log")

        // when
        val logsList = parser.parseLogs(logsChunk)

        // then
        assertEquals(3, logsList.size)

        logsList.first().apply {
            assertEquals("14.01.2019 12:20:05.242", timestamp)
            assertEquals("6fe84dd875d8ca95b4f061a57b3c815d", checksum)
        }
    }

    @Test
    fun shouldAggregateOverlappingLogsBetweenRequests() {
        // given
        val source = MockSource(
                "com/cognifide/gradle/aem/test/tail/aggregating/overlapping/first-chunk-error.log",
                "com/cognifide/gradle/aem/test/tail/aggregating/overlapping/second-chunk-error.log")
        val destination = MockDestination()
        val tailer = Tailer(source, destination)

        // when
        tailer.tail()
        tailer.tail()

        // then
        val logs = destination.logsList
        assertEquals(11, logs.size)

        logs.first().apply {
            assertEquals("14.01.2019 12:04:54.613", timestamp)
            assertEquals("d37f9ce5287800493de0b6ef5bb43338", checksum)
        }
        logs.last().apply {
            assertEquals("14.01.2019 12:04:58.773", timestamp)
            assertEquals("68b331e94d5fe5ec182207198b149535", checksum)
        }
    }

    @Test
    fun shouldAggregateWhenThereIsNoOverlapping() {
        // given
        val source = MockSource(
                "com/cognifide/gradle/aem/test/tail/aggregating/disjoint/first-chunk-error.log",
                "com/cognifide/gradle/aem/test/tail/aggregating/disjoint/second-chunk-error.log")
        val destination = MockDestination()
        val tailer = Tailer(source, destination)

        // when
        tailer.tail()
        tailer.tail()

        // then
        val logs = destination.logsList
        assertEquals(10, logs.size)

        logs.first().apply {
            assertEquals("14.01.2019 12:04:54.613", timestamp)
            assertEquals("1310668b557d5e87686385dfd8c82bdb", checksum)
        }
        logs.last().apply {
            assertEquals("14.01.2019 12:04:58.773", timestamp)
            assertEquals("7071e8cc64b8d3821dea686b20ba5b58", checksum)
        }
    }

    @Test
    fun shouldSkipLogsChunkWhenThereAreNoNewLogs() {
        // given
        val source = MockSource(
                "com/cognifide/gradle/aem/test/tail/aggregating/disjoint/first-chunk-error.log",
                "com/cognifide/gradle/aem/test/tail/aggregating/disjoint/first-chunk-error.log")
        val destination = MockDestination()
        val tailer = Tailer(source, destination)

        // when
        tailer.tail()
        tailer.tail()

        // then
        val logs = destination.logsList
        assertEquals(5, logs.size)

        logs.first().apply {
            assertEquals("14.01.2019 12:04:54.613", timestamp)
            assertEquals("1310668b557d5e87686385dfd8c82bdb", checksum)
        }
        logs.last().apply {
            assertEquals("14.01.2019 12:04:58.519", timestamp)
            assertEquals("b410a72d5bc75b608c2c6f0014f9d88b", checksum)
        }

    }


}