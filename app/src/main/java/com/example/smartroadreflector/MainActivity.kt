package com.example.smartroadreflector

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.CellLocation.requestLocationUpdate
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
import kotlin.random.Random

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var userLocationCircle: Circle? = null
    private var randomCircles: MutableList<Circle> = mutableListOf()
    private var isFirstLocationUpdate = true // 🔹 최초 1회 위치 설정용 변수
    private var receiveCount = 0
    private var currentUserId: String? = null // 로그인한 사용자 ID

    private var simulatedLocation = LatLng(37.67677149273746, 126.74587662812687)
    private var pendingLatOffset = 0.0
    private var pendingLngOffset = 0.0

    private var initialZoomLevel: Float = 15f

    private val locationUpdateHandler = Handler(Looper.getMainLooper())
    private val locationUpdateRunnable = object : Runnable {
        override fun run() {
            val updatedLocation = if (currentUserId == "admin") {
                applyPendingAdminMovement()
                simulatedLocation
            } else {
                requestLocationUpdate()
                userLocationCircle?.center // 현재 GPS 위치
            }

            updatedLocation?.let { updateRandomCircles(it) } // 🔹 사용자가 화면을 조작하는 도중에도 플롯 갱신

            locationUpdateHandler.postDelayed(this, 500) // 0.5초마다 실행
        }
    }

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
                if (currentUserId != "admin") {
                    locationResult.lastLocation?.let { location ->
                        updateMapLocation(location)
                    }
                }
            }
        }

        currentUserId = intent.getStringExtra("USER_ID")

        locationUpdateHandler.post(locationUpdateRunnable)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(simulatedLocation, initialZoomLevel)) // 🔹 최초 1회만 실행
        startLocationUpdates()
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
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, initialZoomLevel)) // 🔹 최초 1회만 지도 이동
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
    }

    private fun applyPendingAdminMovement(): LatLng {
        if (pendingLatOffset != 0.0 || pendingLngOffset != 0.0) {
            simulatedLocation = LatLng(
                simulatedLocation.latitude + pendingLatOffset,
                simulatedLocation.longitude + pendingLngOffset
            )
            pendingLatOffset = 0.0
            pendingLngOffset = 0.0
        }
        updateAdminMapLocation()
        return simulatedLocation
    }

    private fun updateAdminMapLocation() {
        userLocationCircle?.remove()
        userLocationCircle = googleMap?.addCircle(
            CircleOptions()
                .center(simulatedLocation)
                .radius(20.0)
                .strokeColor(0xFFFF0000.toInt())
                .fillColor(0x33FF0000.toInt())
        )
    }

    private fun updateRandomCircles(center: LatLng) {
        // 🔹 사용자가 화면을 조작하는 도중에도 플롯은 갱신되지만, 지도 이동을 강제하지 않음
        randomCircles.forEach { it.remove() }
        randomCircles.clear()

        repeat(3) {
            val randomLatLng = LatLng(
                center.latitude + Random.nextDouble(-0.0009, 0.0009),
                center.longitude + Random.nextDouble(-0.0009, 0.0009)
            )

            val circle = googleMap?.addCircle(
                CircleOptions()
                    .center(randomLatLng)
                    .radius(10.0)
                    .strokeColor(0xFF0000FF.toInt())
                    .fillColor(0x330000FF.toInt())
            )
            circle?.let { randomCircles.add(it) }
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        locationUpdateHandler.removeCallbacks(locationUpdateRunnable)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
