package com.udacity.project4.locationreminders.data.local

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

class FakeDataSource(private val reminders: MutableList<ReminderDTO>) : ReminderDataSource {

    var shouldFail: Boolean = false

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return if (shouldFail)
            Result.Error("Test Exception")
        else
            Result.Success(reminders)
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        val reminder = reminders.find { it.id == id }
        return when {
            shouldFail -> Result.Error("Test Exception")
            reminder == null -> Result.Error("Not found")
            else -> Result.Success(reminder)
        }
    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }


}