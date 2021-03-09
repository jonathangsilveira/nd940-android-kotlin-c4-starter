package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    private lateinit var repository: RemindersLocalRepository

    @Before
    fun setUp() {
        // Using an in-memory database so that the information stored here disappears when the
        // process is killed.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries()
            .build()
        repository = RemindersLocalRepository(
            database.reminderDao(),
            Dispatchers.Main
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getReminderById_error() = runBlocking {
        val id = ""

        val result = repository.getReminder(id) as Result.Error

        MatcherAssert.assertThat(result, instanceOf(Result.Error::class.java))
    }

    @Test
    fun getReminderById_success() = runBlocking {
        val id = "1"
        val reminder1 = ReminderDTO(
            title = "Reminder 1",
            description = "Reminder 1",
            location = "Reminder 1",
            latitude = 0.0,
            longitude = 0.0,
            id = id
        )
        repository.saveReminder(reminder1)

        val result = repository.getReminder(id)

        MatcherAssert.assertThat(result, instanceOf(Result.Success::class.java))
    }

    @Test
    fun getReminders_success() = runBlocking {
        // GIVEN - Insert a reminder.
        val reminder1 = ReminderDTO(
            title = "Reminder 1",
            description = "Reminder 1",
            location = "Reminder 1",
            latitude = 0.0,
            longitude = 0.0,
            id = "1"
        )
        val reminder2 = ReminderDTO(
            title = "Reminder 2",
            description = "Reminder 2",
            location = "Reminder 2",
            latitude = 0.0,
            longitude = 0.0,
            id = "2"
        )
        repository.saveReminder(reminder1)
        repository.saveReminder(reminder2)

        // WHEN - Get the reminders from the database.
        val result = repository.getReminders()

        // THEN - The reminders' table is not empty.
        MatcherAssert.assertThat(result, instanceOf(Result.Success::class.java))
    }

}