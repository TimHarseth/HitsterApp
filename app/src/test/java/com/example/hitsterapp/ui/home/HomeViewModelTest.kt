package com.example.hitsterapp.ui.home

import com.example.hitsterapp.data.repository.PlaylistRepository
import com.example.hitsterapp.data.repository.PlaylistUiModel
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: PlaylistRepository = mockk(relaxed = true)
    private lateinit var viewModel: HomeViewModel

    private val samplePlaylists = listOf(
        PlaylistUiModel("p1", "Rock Hits", "https://url1", 50, 10),
        PlaylistUiModel("p2", "Pop Mix", "https://url2", 30, 0)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.observePlaylists() } returns flowOf(samplePlaylists)
        viewModel = HomeViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `playlists are exposed from repository`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(samplePlaylists, viewModel.playlists.value)
    }

    @Test
    fun `addPlaylist delegates to repository`() = runTest {
        viewModel.addPlaylist("https://open.spotify.com/playlist/abc")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { repository.fetchAndSavePlaylist("https://open.spotify.com/playlist/abc") }
    }

    @Test
    fun `deletePlaylist delegates to repository`() = runTest {
        viewModel.deletePlaylist("p1")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { repository.deletePlaylist("p1") }
    }

    @Test
    fun `resetPlaylist delegates to repository`() = runTest {
        viewModel.resetPlaylist("p1")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { repository.resetPlaylistProgress("p1") }
    }
}
