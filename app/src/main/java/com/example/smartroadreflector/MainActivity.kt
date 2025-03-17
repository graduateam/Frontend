package com.example.smartroadreflector

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.smartroadreflector.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var userLocationCircle: Circle? = null
    private var isFirstLocationUpdate = true
    private var receiveCount = 0
    private var currentUserId: String? = null // 로그인한 사용자 ID

    // 🔹 admin 가상의 좌표값 (초기값: 서울)
    private var simulatedLocation = LatLng(37.5665, 126.9780)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val drawerLayout: DrawerLayout = binding.drawerLayout

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_my_page, MyPageFragment())
            .commit()

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_settings, SettingsFragment())
            .commit()

        binding.mainUserButton.setOnClickListener {
            drawerLayout.openDrawer(binding.fragmentContainerMyPage)
        }

        binding.mainSettingButton.setOnClickListener {
            drawerLayout.openDrawer(binding.fragmentContainerSettings)
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapView2) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.create().apply {
            interval = 2000
            fastestInterval = 1000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (currentUserId != "admin") { // 🔹 일반 사용자만 GPS 사용
                    locationResult.lastLocation?.let { location ->
                        updateMapLocation(location)
                    }
                }
            }
        }

        currentUserId = intent.getStringExtra("USER_ID")

    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        if (currentUserId == "admin") {
            updateAdminMapLocation()
        } else {
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
            return
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun updateMapLocation(location: Location) {
        val myLatLng = LatLng(location.latitude, location.longitude)

        if (isFirstLocationUpdate) {
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 15f))
            isFirstLocationUpdate = false
        }

        userLocationCircle?.remove()
        userLocationCircle = googleMap?.addCircle(
            CircleOptions()
                .center(myLatLng)
                .radius(20.0)
                .strokeColor(0xFFFF0000.toInt())
                .fillColor(0x33FF0000.toInt())
        )

        receiveCount++

        android.util.Log.d("MainActivity", "🗺️ 현재 위치 업데이트: ${myLatLng.latitude}, ${myLatLng.longitude}")
    }

    // 🔹 Admin 모드 시, 가상 위치를 업데이트하는 함수
    private fun updateAdminMapLocation() {
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(simulatedLocation, 15f))

        userLocationCircle?.remove()
        userLocationCircle = googleMap?.addCircle(
            CircleOptions()
                .center(simulatedLocation)
                .radius(20.0)
                .strokeColor(0xFFFF0000.toInt())
                .fillColor(0x33FF0000.toInt())
        )
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (currentUserId == "admin") {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> moveSimulatedLocation(0.00005, 0.0)  // 위로 5m
                KeyEvent.KEYCODE_DPAD_DOWN -> moveSimulatedLocation(-0.00005, 0.0) // 아래로 5m
                KeyEvent.KEYCODE_DPAD_LEFT -> moveSimulatedLocation(0.0, -0.00005) // 왼쪽으로 5m
                KeyEvent.KEYCODE_DPAD_RIGHT -> moveSimulatedLocation(0.0, 0.00005) // 오른쪽으로 5m
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun moveSimulatedLocation(latOffset: Double, lngOffset: Double) {
        simulatedLocation = LatLng(simulatedLocation.latitude + latOffset, simulatedLocation.longitude + lngOffset)
        updateAdminMapLocation()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (currentUserId != "admin") {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
