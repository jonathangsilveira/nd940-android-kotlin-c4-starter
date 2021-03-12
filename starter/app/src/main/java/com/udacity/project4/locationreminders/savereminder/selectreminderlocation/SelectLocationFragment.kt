package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private var map: GoogleMap? = null

    // The entry point to the Fused Location Provider.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_select_location,
            container,
            false
        )

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        binding.saveButton.setOnClickListener { onLocationSelected() }
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        return binding.root
    }

    private fun onLocationSelected() {
        findNavController().popBackStack()
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map?.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map?.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map?.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map?.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        map = googleMap

        setLongClick(googleMap)
        setPoiClick(googleMap)
        setMapStyle(googleMap)
        //enableMyLocation()
    }

    /*override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray) {
        // Check if location permissions are granted and if so enable the
        // location data layer.
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                enableMyLocation()
            }
        }
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map?.isMyLocationEnabled = true
            getDeviceLocation()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }*/

    private fun setPoiClick(map: GoogleMap?) {
        map ?: return
        map.setOnPoiClickListener { poi ->
            map.clear()

            map.createMarker(poi.latLng, poi.name).showInfoWindow()
            saveSelectedLocation(poi.latLng, poi.name, poi)
        }
    }

    private fun setMapStyle(map: GoogleMap?) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            map?.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private fun getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (map?.isMyLocationEnabled == true) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        val lastKnownLocation = task.result ?: return@addOnCompleteListener
                        moveCameraToLocation(
                            LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude)
                        )
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.")
                        Log.e(TAG, "Exception: %s", task.exception)
                        val sidneyAustralia = LatLng(-33.8523341, 151.2106085)
                        moveCameraToLocation(sidneyAustralia)
                        map?.uiSettings?.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun moveCameraToLocation(latLng: LatLng) {
        map?.moveCamera(
            CameraUpdateFactory.newLatLngZoom(latLng, 18f)
        )
    }

    private fun setLongClick(map: GoogleMap?) {
        map ?: return
        map.setOnMapLongClickListener { latLng ->
            map.clear()
            val locationName = getString(R.string.custom_location)
            map.createMarker(latLng, locationName).showInfoWindow()
            saveSelectedLocation(latLng, locationName)
        }
    }

    private fun GoogleMap.createMarker(latLng: LatLng, title: String): Marker {
        return addMarker(
            MarkerOptions()
                .position(latLng)
                .title(title)
        )
    }

    private fun saveSelectedLocation(
        latLng: LatLng,
        name: String,
        poi: PointOfInterest? = null
    ) {
        _viewModel.selectedPOI.value = poi
        _viewModel.latitude.value = latLng.latitude
        _viewModel.longitude.value = latLng.longitude
        _viewModel.reminderSelectedLocationStr.value = name
    }

    companion object {
        private val TAG = SelectLocationFragment::class.java.simpleName
    }

}
