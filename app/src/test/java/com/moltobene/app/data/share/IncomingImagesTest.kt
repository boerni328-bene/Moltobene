package com.moltobene.app.data.share

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** „Teilen mit…“ (#46): Welche geteilten Adressen angenommen werden. */
@RunWith(RobolectricTestRunner::class)
class IncomingImagesTest {

    private val own = "com.moltobene.app.fileprovider"
    private val photo = Uri.parse("content://media/external/images/media/12")
    private val screenshot = Uri.parse("content://com.whatsapp.provider/bild.jpg")

    private fun send(vararg uris: Uri, type: String = "image/jpeg") = Intent(Intent.ACTION_SEND).apply {
        setType(type)
        putExtra(Intent.EXTRA_STREAM, uris.first())
    }

    private fun sendMultiple(vararg uris: Uri) = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "image/*"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris.toList()))
    }

    @Test
    fun einOderMehrereBilder() {
        assertEquals(listOf(photo), IncomingImages.from(send(photo), own))
        assertEquals(listOf(photo, screenshot), IncomingImages.from(sendMultiple(photo, screenshot, photo), own))
    }

    @Test
    fun nurContentAdressenAndererApps() {
        val file = Uri.parse("file:///data/data/com.moltobene.app/databases/moltobene.db")
        val ownFile = Uri.parse("content://$own/camera/capture.jpg")
        assertEquals(listOf(photo), IncomingImages.from(sendMultiple(file, ownFile, photo), own))
    }

    @Test
    fun keineBilderOderKeinTeilen() {
        assertEquals(emptyList<Uri>(), IncomingImages.from(send(photo, type = "text/plain"), own))
        assertEquals(emptyList<Uri>(), IncomingImages.from(Intent(Intent.ACTION_MAIN), own))
        assertEquals(emptyList<Uri>(), IncomingImages.from(null, own))
    }

    @Test
    fun bilderNurInClipData() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            clipData = ClipData.newRawUri("", screenshot)
        }
        assertEquals(listOf(screenshot), IncomingImages.from(intent, own))
    }
}
