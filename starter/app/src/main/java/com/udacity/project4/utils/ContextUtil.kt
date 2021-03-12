package com.udacity.project4.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment

fun Context.checkPermissionGranted(permission: String): Boolean =
    ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun Fragment.checkPermissionGranted(permission: String): Boolean =
    requireActivity().checkPermissionGranted(permission)