package com.example.mapfullintegrationdemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var smf: SupportMapFragment
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private val permissionId = 100
    private lateinit var mMap: GoogleMap
    lateinit var pickup: EditText
    lateinit var drop: EditText
    lateinit var onClick: TextView
    var pickuplat: Double? = null
    var pickuplong: Double? = null
    var droplat: Double? = null
    var droplong: Double? = null
    var pickuplatlong: LatLng? = null
    var droplatlong: LatLng? = null
    var Currentlocations: LatLng? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        pickup = findViewById(R.id.pickup)
        drop = findViewById(R.id.drop)
        onClick = findViewById(R.id.onClick)

        //Google place Api
        if (!Places.isInitialized()) {
            Places.initialize(this, "AIzaSyDFPQu9rxw0T1FEkxpeTZMjOawBaqVcJzc")
        }



        getLocationPermission()

        if (checkPermissions()) {
            Log.e("Location", ":11111111 ")
            if (isLocationEnabled()) {


            } else {
                Toast.makeText(this, "Please turn on location", Toast.LENGTH_LONG)
                    .show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }

        pickup.setOnClickListener {


            val fields: List<Place.Field> =
                Arrays.asList<Place.Field>(Place.Field.ID, Place.Field.ADDRESS, Place.Field.LAT_LNG)

            // Start the autocomplete intent.
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                .build(baseContext)
            startActivityForResult(intent, 1)

        }

        drop.setOnClickListener {
            val fields: List<Place.Field> =
                Arrays.asList<Place.Field>(Place.Field.ID, Place.Field.ADDRESS, Place.Field.LAT_LNG)

            // Start the autocomplete intent.
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                .build(baseContext)
            startActivityForResult(intent, 2)

        }
        onClick.setOnClickListener {
            val intent = Intent(baseContext, ChipNavigationBarActivity::class.java)
            startActivity(intent)
        }


////        //Runtime permissions
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), permissionId)
//        }


        smf = supportFragmentManager.findFragmentById(R.id.my_ides_map) as SupportMapFragment
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        getLocations()


    }


    private fun getLocationPermission() {
        if (checkPermissions()) {
            Log.e("Location", ":11111111 ")
            if (isLocationEnabled()) {
                Toast.makeText(this, "Permission Guaranteed ", Toast.LENGTH_LONG)
                    .show()
            } else {
                Toast.makeText(this, "Please turn on location", Toast.LENGTH_LONG)
                    .show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            this?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            permissionId
        )
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == permissionId) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLocationPermission()
            }
        }
    }

    private fun getLocations() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                permissionId
            )
            return
        }
        mFusedLocationClient.lastLocation.addOnCompleteListener { task ->
            val location: Location? = task.result
            if (location != null) {
                smf.getMapAsync { googlemap: GoogleMap ->
                    mMap = googlemap
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val lists: MutableList<Address> =
                        geocoder.getFromLocation(
                            location.latitude,
                            location.longitude,
                            1
                        ) as MutableList<Address>
                    val address = "\n${lists[0].getAddressLine(0)}"
                    pickup.setText(address)
                    Currentlocations = LatLng(location.latitude, location.longitude)
                    pickuplatlong = Currentlocations
                    mMap.addMarker(
                        MarkerOptions().position(Currentlocations!!).title("Current Location")
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.locationcar))
                    )
//                    mMap.addMarker(MarkerOptions().position(Currentlocations!!).title("Current Location"))
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(Currentlocations!!, 18f))
                    Log.e(
                        "TAGThjnhknk",
                        "onActivityResult: " + Currentlocations + " " + pickuplatlong
                    )
                }
            }
        }
    }


    private fun getDirectionURL(locationp: LatLng, locationd: LatLng, secret: String): String {
        return "https://maps.googleapis.com/maps/api/directions/json?origin=${locationp.latitude},${locationp.longitude}" +
                "&destination=${locationd.latitude},${locationd.longitude}" +
                "&sensor=false" +
                "&mode=driving" +
                "&key=$secret"
    }

    @SuppressLint("StaticFieldLeak")
    private inner class GetDirection(val url: String) :
        AsyncTask<Void, Void, List<List<LatLng>>>() {
        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg params: Void?): List<List<LatLng>> {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val data = response.body!!.string()

            val result = ArrayList<List<LatLng>>()
            try {
                val respObj = Gson().fromJson(data, MapData::class.java)
                val path = ArrayList<LatLng>()
                for (i in 0 until respObj.routes[0].legs[0].steps.size) {
                    path.addAll(decodePolyline(respObj.routes[0].legs[0].steps[i].polyline.points))

                }
                result.add(path)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return result
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(result: List<List<LatLng>>) {
            val lineoption = PolylineOptions()
            for (i in result.indices) {
                lineoption.addAll(result[i])
                lineoption.width(10f)
                lineoption.color(Color.RED)
                lineoption.geodesic(true)
            }
            mMap.addPolyline(lineoption)
        }
    }

    fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            val latLng = LatLng((lat.toDouble() / 1E5), (lng.toDouble() / 1E5))
            poly.add(latLng)
        }
        return poly
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        try {
            if (requestCode == 1) {
                mMap.clear()
                pickuplatlong = Currentlocations

                Log.e("hhhvhjhhhvhj", "onActivityResult: " + pickuplatlong + " " + Currentlocations)

                val place = data?.let { Autocomplete.getPlaceFromIntent(it) }
                val adddress = place!!.address
                pickuplat = place.latLng.latitude
                pickuplong = place.latLng.longitude
                pickup.setText(adddress)
                Log.i(
                    "TAGTAGTAG",
                    "Place: " + place.name + ", " + place.address + ", " + place.latLng.latitude
                )

                pickuplatlong = LatLng(pickuplat!!.toDouble(), pickuplong!!.toDouble())
                mMap.addMarker(MarkerOptions().position(pickuplatlong!!).title("Pickup Location"))
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pickuplatlong!!, 18f))


//            //Maps Root Draw

                val urll = getDirectionURL(
                    pickuplatlong!!,
                    droplatlong!!,
                    "AIzaSyDFPQu9rxw0T1FEkxpeTZMjOawBaqVcJzc"
                )
                GetDirection(urll).execute()//
            }
        } catch (E: Exception) {

        }

        try {
            if (requestCode == 2) {
                mMap.clear()

                val place = data?.let { Autocomplete.getPlaceFromIntent(it) }
                val adddress = place!!.address
                droplat = place.latLng.latitude
                droplong = place.latLng.longitude
                Log.i(
                    "TAGTAGTAG",
                    "Place: " + place.name + ", " + place.address + ", " + place.latLng.latitude
                )
                drop.setText(adddress)
                droplatlong = LatLng(droplat!!.toDouble(), droplong!!.toDouble())
                mMap.addMarker(MarkerOptions().position(droplatlong!!).title("Drop Location"))
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(droplatlong!!, 18f))

//            Maps Root Draw
                val urll = getDirectionURL(
                    pickuplatlong!!,
                    droplatlong!!,
                    "AIzaSyDFPQu9rxw0T1FEkxpeTZMjOawBaqVcJzc"
                )
                GetDirection(urll).execute()
            }
        } catch (E: Exception) {

        }
    }
}