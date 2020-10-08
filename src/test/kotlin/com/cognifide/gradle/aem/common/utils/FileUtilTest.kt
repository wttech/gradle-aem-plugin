package com.cognifide.gradle.aem.common.utils

import com.cognifide.gradle.aem.common.utils.FileUtil.sanitizePath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FileUtilTest {

    @Test
    fun `should sanitize path properly`() {
        assertEquals("/home/punky/gap/a_b/c/d/e", sanitizePath("/home/punky/gap/a_b/c/d/e"))
        assertEquals("/home/punky/gap/a_b/c/d/e", sanitizePath("/home/punky/gap/a b/c/d/e"))
        assertEquals("/home/punky/gap/a_b/c/d/e", sanitizePath("/home/punky/gap/a&b/c/d/e"))
        assertEquals("C:/gap/a_b/c/d/e", sanitizePath("""C:\gap\a&b\c\d\e"""))
        assertEquals("C:/gap/a_b/c/d/e", sanitizePath("C:/gap/a&b/c/d/e"))
    }
}
