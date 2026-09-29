package com.moltobene.app

import org.junit.Assert.assertTrue
import org.junit.Test

/** Erster automatischer Test: Die Versionsnummer folgt dem Schema Major.Minor.Patch. */
class VersionTest {

    @Test
    fun versionNameFolgtMajorMinorPatch() {
        val versionName = BuildConfig.VERSION_NAME
        assertTrue(
            "versionName \"$versionName\" entspricht nicht Major.Minor.Patch",
            Regex("""\d+\.\d+\.\d+""").matches(versionName),
        )
    }

    @Test
    fun versionCodeIstPositiv() {
        assertTrue(BuildConfig.VERSION_CODE > 0)
    }
}
