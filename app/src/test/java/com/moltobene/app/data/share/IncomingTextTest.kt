package com.moltobene.app.data.share

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** „Teilen mit…“ für Text: Was aus anderen Apps angenommen wird. */
@RunWith(RobolectricTestRunner::class)
class IncomingTextTest {

    private fun send(text: String?, subject: String? = null, type: String = "text/plain") =
        Intent(Intent.ACTION_SEND).apply {
            setType(type)
            text?.let { putExtra(Intent.EXTRA_TEXT, it) }
            subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
        }

    @Test
    fun textUndBetreff() {
        val shared = IncomingText.from(send("  Zutaten\n500 g Mehl \n", subject = " Brot "))
        assertEquals(IncomingText.Shared("Zutaten\n500 g Mehl", "Brot"), shared)
        assertNull(IncomingText.from(send("Zutaten", subject = "  "))?.subject)
    }

    @Test
    fun leererOderFremderInhaltWirdIgnoriert() {
        assertNull(IncomingText.from(send(null)))
        assertNull(IncomingText.from(send("   ")))
        assertNull(IncomingText.from(send("Zutaten", type = "image/jpeg")))
        assertNull(IncomingText.from(Intent(Intent.ACTION_VIEW).setType("text/plain")))
        assertNull(IncomingText.from(null))
    }

    @Test
    fun sehrLangerTextWirdGekuerzt() {
        val shared = IncomingText.from(send("a".repeat(IncomingText.MAX_CHARS + 100)))
        assertEquals(IncomingText.MAX_CHARS, shared?.text?.length)
    }
}
