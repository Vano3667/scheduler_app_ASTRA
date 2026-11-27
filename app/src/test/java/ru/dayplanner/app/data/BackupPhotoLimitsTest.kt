package ru.dayplanner.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupPhotoLimitsTest {
    private fun accepts(w: Int = 1, h: Int = 1, bytes: Long = 1, mime: String? = "image/jpeg") =
        BackupPhotoLimits.accepts(bytes, w, h, mime)

    @Test fun byteLimitsAreInclusive() {
        assertFalse(accepts(bytes = 0)); assertTrue(accepts(bytes = 1))
        assertTrue(accepts(bytes = 128L*1024*1024)); assertFalse(accepts(bytes = 128L*1024*1024+1))
    }

    @Test fun dimensionsAndPixelBudgetAreIndependent() {
        assertFalse(accepts(0, 1)); assertFalse(accepts(1, -1))
        assertTrue(accepts(65_535, 1)); assertFalse(accepts(1, 65_536))
        assertTrue(accepts(16_384, 16_384)); assertFalse(accepts(16_385, 16_384))
        assertFalse(accepts(65_535, 65_535)) // Произведение не помещается в Int.
    }

    @Test fun useDecoderMimeNotFileExtensionOrFixedFormatList() {
        for (mime in listOf("image/jpeg", "image/png", "image/webp", "image/gif", "image/heif", "image/avif", "image/x-adobe-dng")) assertTrue(accepts(mime = mime))
        for (mime in listOf(null, "", "image/", "image/ ", "application/octet-stream", "text/plain")) assertFalse(accepts(mime = mime))
    }
}
