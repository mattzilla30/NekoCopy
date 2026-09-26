package org.nekomanga.presentation.components.storage

import com.hippo.unifile.UniFile
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nekomanga.domain.storage.StorageManager

class StorageLocationTest {

    @Test
    fun `a location that creates the saved folder can host the app directories`() {
        val directory = mockk<UniFile>(relaxed = true)
        every { directory.createDirectory(StorageManager.SAVED_DIR) } returns mockk(relaxed = true)

        assertTrue(directory.canHostAppDirectories())
    }

    @Test
    fun `a location whose provider refuses the folder cannot host the app directories`() {
        val directory = mockk<UniFile>(relaxed = true)
        every { directory.createDirectory(StorageManager.SAVED_DIR) } returns null

        assertFalse(directory.canHostAppDirectories())
    }

    @Test
    fun `a location whose provider throws cannot host the app directories`() {
        val directory = mockk<UniFile>(relaxed = true)
        every { directory.createDirectory(StorageManager.SAVED_DIR) } throws
            UnsupportedOperationException("createDocument not supported")

        assertFalse(directory.canHostAppDirectories())
    }
}
