package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.FakeDataSource
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
@ExperimentalCoroutinesApi
@MediumTest
class ReminderListFragmentTest : KoinTest {

    private lateinit var database: RemindersDatabase
    private val dataSource: FakeDataSource by inject()

    private val testModules = module {
        viewModel {
            RemindersListViewModel(
                get(),
                get() as FakeDataSource
            )
        }
        single {
            SaveReminderViewModel(
                get(),
                get() as FakeDataSource
            )
        }
        single {
            database = Room.inMemoryDatabaseBuilder(
                getApplicationContext(),
                RemindersDatabase::class.java
            ).allowMainThreadQueries().build()
        }
        single { FakeDataSource(mutableListOf()) }
        single { database.reminderDao() }
    }

    @Before
    fun setUp() {
        stopKoin()
        startKoin {
            androidContext(getApplicationContext<Application>())
            modules(listOf(testModules))
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun reminderListFragment_noData() {
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.Theme_LocationReminders_Light)
        val navController = mock(NavController::class.java)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        onView(withId(R.id.noDataTextView)).check(matches(withText("No Data")))
    }

    @Test
    fun reminderListFragment_withReminders() = runBlockingTest {
        val reminder = ReminderDTO("Reminder 1", "First Reminder", "Mout'S", -26.92025694308035, -49.06450636931977)
        dataSource.saveReminder(reminder)

        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.Theme_LocationReminders_Light)
        val navController = mock(NavController::class.java)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        onView(withText("Reminder 1")).check(matches(isDisplayed()))
    }

    @Test
    fun reminderListFragment_navigateToSaveReminder() {
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.Theme_LocationReminders_Light)
        val navController = mock(NavController::class.java)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        onView(withId(R.id.addReminderFAB)).perform(click())

        verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder()
        )
    }
}