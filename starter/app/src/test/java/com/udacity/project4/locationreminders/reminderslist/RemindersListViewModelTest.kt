package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.utils.asDomain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.mockito.Mockito
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val reminder1 = ReminderDTO("Reminder 1", "Reminder 1", "Reminder 1", 0.0, 0.0, "1")
    private val reminder2 = ReminderDTO("Reminder 2", "Reminder 2", "Reminder 2", 0.0, 0.0, "2")
    private val reminder3 = ReminderDTO("Reminder 3", "Reminder 3", "Reminder 3", 0.0, 0.0, "3")
    private val testReminders = listOf(reminder1, reminder2, reminder3)

    private lateinit var dataSource: FakeDataSource

    private lateinit var viewModel: RemindersListViewModel

    @Before
    fun setUp() {
        val applicationMock = Mockito.mock(Application::class.java)
        dataSource = FakeDataSource(testReminders.toMutableList())
        viewModel = RemindersListViewModel(applicationMock, dataSource)
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun loadReminders_loading() = runBlockingTest {
        // Pause dispatcher so you can verify initial values.
        mainCoroutineRule.pauseDispatcher()

        // Load the reminders in the view model.
        viewModel.loadReminders()

        // Then assert that the progress indicator is shown.
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))

        // Execute pending coroutines actions.
        mainCoroutineRule.resumeDispatcher()

        // Then assert that the progress indicator is hidden.
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun loadReminders_withSuccess() = runBlockingTest {
        // Tells the data source to not fail the execution.
        dataSource.shouldFail = false
        val expected = testReminders.map { it.asDomain() }

        // Load the reminders in the view model.
        viewModel.loadReminders()

        // Then assert that the result matches the expectec list value.
        assertThat(viewModel.remindersList.getOrAwaitValue(), `is`(expected))
    }

    @Test
    fun loadReminders_error() = runBlockingTest {
        // Tells the data source to not fail the execution.
        dataSource.shouldFail = true

        // Pause dispatcher so you can verify initial values.
        mainCoroutineRule.pauseDispatcher()

        // Load the reminders in the view model.
        viewModel.loadReminders()

        /*// Then assert that the progress indicator is shown.
        assertThat(viewModel.showSnackBar.getOrAwaitValue(), nullValue())*/

        // Execute pending coroutines actions.
        mainCoroutineRule.resumeDispatcher()

        // Then assert that the progress indicator is hidden.
        assertThat(viewModel.showSnackBar.getOrAwaitValue(), `is`("Test Exception"))
    }

}