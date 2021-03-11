package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.GeofencingConstants
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var geofencePendingIntent: PendingIntent

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
            // Navigate to another fragment to get the user location
            val directions =
                SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment()
            _viewModel.navigationCommand.value = NavigationCommand.To(directions)
        }
        binding.saveReminder.setOnClickListener {
            val reminder = _viewModel.createReminder()
            _viewModel.validateAndSaveReminder(reminder)
            addGeofenceRequest(reminder)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
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
    }

}
