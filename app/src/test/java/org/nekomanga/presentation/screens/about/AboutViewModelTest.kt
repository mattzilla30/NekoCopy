package org.nekomanga.presentation.screens.about

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.main.AppSnackbarManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.nekomanga.R
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.usecases.preferences.GetFormattedBuildTimeUseCase
import tachiyomi.core.preference.Preference
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

@OptIn(ExperimentalCoroutinesApi::class)
class AboutViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockPreferences: PreferencesHelper
    private lateinit var mockSecurityPreferences: SecurityPreferences
    private lateinit var mockGetFormattedBuildTimeUseCase: GetFormattedBuildTimeUseCase
    private lateinit var mockAppSnackbarManager: AppSnackbarManager

    private lateinit var viewModel: AboutViewModel

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        Dispatchers.setMain(testDispatcher)

        mockPreferences = mockk()
        mockSecurityPreferences = mockk()
        mockGetFormattedBuildTimeUseCase = mockk()
        mockAppSnackbarManager = mockk()

        // Mock defaults needed during initialization of AboutViewModel
        val incognitoModePref = mockk<Preference<Boolean>> { every { get() } returns false }
        every { mockSecurityPreferences.incognitoMode() } returns incognitoModePref
        every { mockGetFormattedBuildTimeUseCase(any()) } returns "Jan 1, 2026"

        Injekt.addSingleton(mockPreferences)
        Injekt.addSingleton(mockSecurityPreferences)
        Injekt.addSingleton(mockGetFormattedBuildTimeUseCase)
        Injekt.addSingleton(mockAppSnackbarManager)

        viewModel = AboutViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `given initial state when initialized then buildTime and incognitoMode are correct`() {
        // Assert
        val state = viewModel.aboutScreenState.value
        assertEquals("Jan 1, 2026", state.buildTime)
        assertFalse(state.incognitoMode)
    }

    @Test
    fun `given version long clicked when called then build info copied snackbar is shown`() =
        runTest {
            // Arrange
            coEvery { mockAppSnackbarManager.showSnackbar(any()) } returns Unit

            // Act
            viewModel.onVersionLongClicked()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) {
                mockAppSnackbarManager.showSnackbar(
                    withArg {
                        assertEquals(R.string._copied_to_clipboard, it.messageRes)
                        assertEquals(R.string.build_information, it.fieldRes)
                    }
                )
            }
        }
}
