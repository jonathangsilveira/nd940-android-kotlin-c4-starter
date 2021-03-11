package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
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
class SaveReminderViewModelTest {

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

    private lateinit var viewModel: SaveReminderViewModel

    @Before
    fun setUp() {
        val applicationMock = Mockito.mock(Application::class.java)
        dataSource = FakeDataSource(testReminders.toMutableList())
        viewModel = SaveReminderViewModel(applicationMock, dataSource)
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun validateEnteredData_withoutTitle() {
        // GIVEN
        val reminder = reminder1.copy(title = null).asDomain()

        val isValid = viewModel.validateEnteredData(reminder)

        assertThat(isValid, `is`(false))
        assertThat(viewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_enter_title))
    }

    @Test
    fun validateEnteredData_withoutLocation() {
        // GIVEN
        val reminder = reminder1.copy(location = null).asDomain()

        val isValid = viewModel.validateEnteredData(reminder)

        assertThat(isValid, `is`(false))
        assertThat(viewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_select_location))
    }

    @Test
    fun saveReminder_success() = runBlockingTest {
        val reminder = reminder1.asDomain()
        dataSource.shouldFail = false

        mainCoroutineRule.pauseDispatcher()

        viewModel.saveReminder(reminder)

        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))

        mainCoroutineRule.resumeDispatcher()

        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))
        assertThat(viewModel.navigationCommand.getOrAwaitValue(), `is`(NavigationCommand.Back))
    }

}