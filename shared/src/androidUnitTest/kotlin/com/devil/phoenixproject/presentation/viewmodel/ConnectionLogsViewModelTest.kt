package com.devil.phoenixproject.presentation.viewmodel

import com.devil.phoenixproject.data.repository.ConnectionLogRepository
import com.devil.phoenixproject.data.repository.LogEventType
import com.devil.phoenixproject.data.repository.LogLevel
import com.devil.phoenixproject.testutil.TestCoroutineRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConnectionLogsViewModelTest {

    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    private lateinit var repository: ConnectionLogRepository
    private lateinit var viewModel: ConnectionLogsViewModel

    @Before
    fun setup() {
        repository = ConnectionLogRepository.instance
        repository.clearAll()
        repository.setEnabled(true)
        viewModel = ConnectionLogsViewModel()
    }

    @Test
    fun `filters logs by level`() = runTest {
        repository.debug(LogEventType.SCAN_START, "Debug log")
        repository.error(LogEventType.CONNECT_FAIL, "Error log")

        viewModel.toggleLevel(LogLevel.DEBUG)
        advanceUntilIdle()

        assertEquals(1, viewModel.logs.value.size)
        assertEquals(LogLevel.ERROR.name, viewModel.logs.value.first().level)
    }

    @Test
    fun `filters logs by search query`() = runTest {
        repository.info(LogEventType.CONNECT_SUCCESS, "Connected to DeviceA")
        repository.info(LogEventType.CONNECT_SUCCESS, "Connected to DeviceB")

        viewModel.setSearchQuery("DeviceA")
        advanceUntilIdle()

        assertEquals(1, viewModel.logs.value.size)
        assertTrue(viewModel.logs.value.first().message.contains("DeviceA"))
    }
}
