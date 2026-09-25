package org.nekomanga.logging

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import timber.log.Timber

class ReleaseLogTreeTest {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.println(any(), any(), any()) } returns 0
        every { Log.getStackTraceString(any()) } answers
            {
                firstArg<Throwable>().message ?: "exception"
            }
        Timber.plant(ReleaseLogTree())
    }

    @After
    fun tearDown() {
        Timber.uprootAll()
        unmockkStatic(Log::class)
    }

    @Test
    fun `log debug level does not write to logcat`() {
        Timber.tag("TestTag").d("Debug Message")

        verify(exactly = 0) { Log.println(any(), any(), any()) }
    }

    @Test
    fun `log info level writes to logcat`() {
        Timber.tag("TestTag").i("Info Message")

        verify(exactly = 1) { Log.println(Log.INFO, "TestTag", "Info Message") }
    }

    @Test
    fun `log warn level writes to logcat`() {
        Timber.tag("TestTag").w("Warn Message")

        verify(exactly = 1) { Log.println(Log.WARN, "TestTag", "Warn Message") }
    }

    @Test
    fun `log error level writes message and stack trace to logcat`() {
        Timber.tag("TestTag").e(Exception("Error Exception"), "Error Message")

        verify(exactly = 1) {
            Log.println(
                Log.ERROR,
                "TestTag",
                match { it.contains("Error Message") && it.contains("Error Exception") },
            )
        }
    }

    @Test
    fun `log error level with CancellationException does not write to logcat`() {
        Timber.tag("TestTag")
            .e(kotlinx.coroutines.CancellationException("Job was cancelled"), "Cancelled")

        verify(exactly = 0) { Log.println(any(), any(), any()) }
    }
}
