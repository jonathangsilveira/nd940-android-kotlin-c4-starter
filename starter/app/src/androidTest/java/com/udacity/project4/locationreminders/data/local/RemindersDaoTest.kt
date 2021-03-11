package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    @Before
    fun initDb() {
        // Using an in-memory database so that the information stored here disappears when the
        // process is killed.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun getReminderById_success() = runBlockingTest {
        // GIVEN - Insert a reminder.
        val dao = database.reminderDao()
        val reminder = ReminderDTO(
            title = "Reminder 1",
            description = "Reminder 1",
            location = "Reminder 1",
            latitude = 0.0,
            longitude = 0.0,
            id = "1"
        )
        dao.saveReminder(reminder)

        // WHEN - Get the reminder by id from the database.
        val localReminder = dao.getReminderById(reminder.id)

        // THEN - The loaded data contains the expected values.
        assertThat(localReminder as ReminderDTO, notNullValue())
        assertThat(localReminder.id, `is`(reminder.id))
        assertThat(localReminder.description, `is`(reminder.description))
        assertThat(localReminder.location, `is`(reminder.location))
        assertThat(localReminder.latitude, `is`(reminder.latitude))
        assertThat(localReminder.longitude, `is`(reminder.longitude))
        assertThat(localReminder.title, `is`(reminder.title))
    }

    @Test
    fun getReminders_success() = runBlockingTest {
        // GIVEN - Insert a reminder.
        val dao = database.reminderDao()
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
        dao.saveReminder(reminder1)
        dao.saveReminder(reminder2)

        // WHEN - Get the reminders from the database.
        val reminders = dao.getReminders()

        // THEN - The reminders' table is not empty.
        assertThat(reminders.isNotEmpty(), `is`(true))
    }

    @Test
    fun deleteAllReminders_success() = runBlockingTest {
        // GIVEN - Insert a reminder.
        val dao = database.reminderDao()
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
        dao.saveReminder(reminder1)
        dao.saveReminder(reminder2)

        // WHEN - Delete all reminders from the database.
        dao.deleteAllReminders()
        val reminders = dao.getReminders()

        // THEN - The reminders' table is empty.
        assertThat(reminders.isEmpty(), `is`(true))
    }

}