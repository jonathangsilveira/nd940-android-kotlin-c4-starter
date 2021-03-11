package com.udacity.project4.utils

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

fun ReminderDTO.asDomain(): ReminderDataItem {
    return ReminderDataItem(
        title = this.title,
        description = this.description,
        location = this.location,
        latitude = this.latitude,
        longitude = this.longitude,
        id = this.id
    )
}