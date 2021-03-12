package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.GeofencingConstants
import com.udacity.project4.utils.checkPermissionGranted
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var geofencePendingIntent: PendingIntent

    private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_save_reminder,
            container,
            false
        )
        setDisplayHomeAsUpEnabled(true)
        binding.viewModel = _viewModel
        val appContext = requireActivity().applicationContext
        val intent = Intent(appContext, GeofenceBroadcastReceiver::class.java)
            .apply { action = ACTION_GEOFENCE_EVENT }
        geofencePendingIntent = PendingIntent.getBroadcast(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            if (foregroundAndBackgroundLocationPermissionApproved())
                navigateToLocationSelection()
            else
                requestForegroundAndBackgroundLocationPermissions()
        }
        binding.saveReminder.setOnClickListener {
            val reminder = _viewModel.createReminder()
            _viewModel.validateAndSaveReminder(reminder)
            addGeofenceRequest(reminder)
        }
    }

    private fun navigateToLocationSelection() {
        // Navigate to another fragment to get the user location
        val directions =
            SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment()
        _viewModel.navigationCommand.value = NavigationCommand.To(directions)
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    override fun onStart() {
        super.onStart()
        checkPermissions()
    }

    /**
     * Starts the permission check and Geofence process only if the Geofence associated with the
     * current hint isn't yet active.
     */
    private fun checkPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettings()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    /*
     *  Requests ACCESS_FINE_LOCATION and (on Android 10+ (Q) ACCESS_BACKGROUND_LOCATION.
     */
    @TargetApi(Build.VERSION_CODES.Q)
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
        Log.d(TAG, "Request foreground only location permission")
        requestPermissions(
            permissionsArray,
            resultCode
        )
    }

    /*
     *  Determines whether the app has the appropriate permissions across Android 10+ and all other
     *  Android versions.
     */
    @TargetApi(Build.VERSION_CODES.Q)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved =
            checkPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                checkPermissionGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    @SuppressLint("MissingPermission")
    private fun addGeofenceRequest(reminder: ReminderDataItem) {
        val geofence = buildGeofence(reminder)
        val geofencingRequest = buildGeofencingRequest(geofence)
        geofencingClient.removeGeofences(geofencePendingIntent)
            ?.run {
                addOnCompleteListener {
                    geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                        ?.run {
                            addOnSuccessListener {
                                Log.e(TAG, "Add Geofence: ${geofence.requestId}")
                            }
                            addOnFailureListener {
                                if ((it.message != null)) {
                                    Log.w(TAG, it.message.orEmpty())
                                }
                            }
                        }
                }
            }
    }

    /*
     *  Uses the Location Client to check the current state of location settings, and gives the user
     *  the opportunity to turn on location services within our app.
     */
    private fun checkDeviceLocationSettings(resolve: Boolean = true) {
        val locationRequest = createLocationRequest()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve){
                try {
                    exception.startResolutionForResult(requireActivity(),
                        REQUEST_TURN_DEVICE_LOCATION_ON)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                showLocationRequiredMessage()
            }
        }
    }

    private fun showLocationRequiredMessage() {
        Snackbar.make(
            binding.saveReminderLayout,
            R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
        ).setAction(android.R.string.ok) {
            checkDeviceLocationSettings()
        }.show()
    }

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
    }

    /*
     * In all cases, we need to have the location permission.  On Android 10+ (Q) we need to have
     * the background permission as well.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionResult")
        if (requiredPermissionsDenied(grantResults, requestCode))
            showPermissionDeniedExplanation()
        else
            checkDeviceLocationSettings()
    }

    private fun requiredPermissionsDenied(
        grantResults: IntArray,
        requestCode: Int
    ): Boolean {
        return permissionRequestCancelled(grantResults) ||
                grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
                (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                        grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED)
    }

    private fun showPermissionDeniedExplanation() {
        Snackbar.make(
            binding.saveReminderLayout,
            R.string.permission_denied_explanation,
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction(R.string.settings) { onConfirmPermissionDeniedExplanation() }
            .show()
    }

    private fun onConfirmPermissionDeniedExplanation() {
        startActivity(Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    private fun permissionRequestCancelled(grantResults: IntArray) = grantResults.isEmpty()

    private fun buildGeofencingRequest(geofence: Geofence?) =
        GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

    private fun buildGeofence(reminder: ReminderDataItem) =
        Geofence.Builder()
            .setRequestId(reminder.id)
            .setCircularRegion(
                requireNotNull(reminder.latitude),
                requireNotNull(reminder.longitude),
                GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
            )
            .setExpirationDuration(GeofencingConstants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

    companion object {
        internal const val ACTION_GEOFENCE_EVENT =
            "SaveReminderFragment.LocationReminders.action.ACTION_GEOFENCE_EVENT"
        private val TAG = SaveReminderFragment::class.java.simpleName
        private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
        private const val LOCATION_PERMISSION_INDEX = 0
        private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
    }

}
