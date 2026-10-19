package com.rkgit.one

import com.rkgit.one.data.files.DownloadException
import com.rkgit.one.ui.UiState
import com.rkgit.one.ui.isOfflineError
import com.rkgit.one.ui.toRefreshMessage
import com.rkgit.one.ui.toUserMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class OfflineLogicTest {

    @Test
    fun isOfflineError_trueForConnectivityExceptions() {
        assertTrue(UnknownHostException().isOfflineError())
        assertTrue(SocketTimeoutException().isOfflineError())
        assertTrue(ConnectException().isOfflineError())
        assertTrue(IOException("raw IO error").isOfflineError())
        assertTrue(RuntimeException(UnknownHostException()).isOfflineError())
        assertTrue(RuntimeException(IllegalStateException(UnknownHostException())).isOfflineError())
    }

    @Test
    fun isOfflineError_falseForNonConnectivityExceptions() {
        assertFalse(IllegalStateException().isOfflineError())
        assertFalse(IllegalArgumentException().isOfflineError())
        assertFalse(RuntimeException().isOfflineError())
        assertFalse(DownloadException("x").isOfflineError())
        assertFalse(RuntimeException(DownloadException("x")).isOfflineError())
    }

    @Test
    fun toUserMessage_returnsCorrectMessages() {
        val offline = UnknownHostException()
        val nonOffline = IllegalStateException()
        val downloadEx = DownloadException("disk full")

        assertEquals(
            "Can't reach the server. Check your internet connection and try again.",
            offline.toUserMessage()
        )
        assertEquals(
            "Something went wrong. Please try again.",
            nonOffline.toUserMessage()
        )
        assertEquals(
            "Something went wrong. Please try again.",
            downloadEx.toUserMessage()
        )
    }

    @Test
    fun toRefreshMessage_returnsCorrectMessages() {
        val offline = UnknownHostException()
        val nonOffline = IllegalStateException()
        val downloadEx = DownloadException("disk full")

        assertEquals(
            "Can't refresh. You're offline.",
            offline.toRefreshMessage()
        )
        assertEquals(
            "Couldn't refresh. Please try again.",
            nonOffline.toRefreshMessage()
        )
        assertEquals(
            "Couldn't refresh. Please try again.",
            downloadEx.toRefreshMessage()
        )
    }

    @Test
    fun uiStateError_offlineDefaultsToFalse() {
        val error = UiState.Error("Some error")
        assertEquals("Some error", error.message)
        assertFalse(error.offline)
    }
}
