package com.example.hitsterapp.ui.player

import androidx.lifecycle.SavedStateHandle
import com.example.hitsterapp.data.db.entity.TrackEntity
import com.example.hitsterapp.data.repository.PlaylistRepository
import com.example.hitsterapp.data.repository.SpotifyRemoteRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: PlaylistRepository = mockk(relaxed = true)
    private val remoteRepository: SpotifyRemoteRepository = mockk(relaxed = true)

    private val sampleTrack = TrackEntity("t1", "p1", "Song One", "Artist A", 1990)

    private fun buildViewModel(): PlayerViewModel {
        val savedState = SavedStateHandle(mapOf("playlistId" to "p1"))
        return PlayerViewModel(repository, remoteRepository, savedState)
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { remoteRepository.connect() } returns true
        coEvery { repository.getRandomUnplayedTrack("p1") } returns sampleTrack
        coEvery { repository.getPlayedCount("p1") } returns 0
        coEvery { repository.getTrackCount("p1") } returns 10
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts with hidden song and playing state`() = runTest {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.isRevealed)
        assertTrue(vm.uiState.value.isPlaying)
        assertEquals("t1", vm.uiState.value.currentTrack?.id)
    }

    @Test
    fun `reveal sets isRevealed to true`() = runTest {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.reveal()
        assertTrue(vm.uiState.value.isRevealed)
    }

    @Test
    fun `togglePlayPause pauses when playing`() = runTest {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.togglePlayPause()
        assertFalse(vm.uiState.value.isPlaying)
        coVerify { remoteRepository.pause() }
    }

    @Test
    fun `next marks track as played and loads next`() = runTest {
        val secondTrack = TrackEntity("t2", "p1", "Song Two", "Artist B", 2000)
        coEvery { repository.getRandomUnplayedTrack("p1") } returnsMany listOf(sampleTrack, secondTrack)

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.next()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.markTrackAsPlayed("p1", "t1") }
        assertEquals("t2", vm.uiState.value.currentTrack?.id)
        assertFalse(vm.uiState.value.isRevealed)
    }

    @Test
    fun `next sets isComplete when no more tracks`() = runTest {
        coEvery { repository.getRandomUnplayedTrack("p1") } returnsMany listOf(sampleTrack, null)
        coEvery { repository.getTrackCount("p1") } returns 10
        coEvery { repository.getPlayedCount("p1") } returns 10

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.next()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.isComplete)
        assertEquals(10, vm.uiState.value.totalCount)
    }
}
