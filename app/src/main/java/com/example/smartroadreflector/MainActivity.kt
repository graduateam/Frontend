package com.example.smartroadreflector

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
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
import kotlin.random.Random

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var userLocationCircle: Circle? = null
    private var randomCircles: MutableList<Circle> = mutableListOf()
    private var isFirstLocationUpdate = true
    private var receiveCount = 0
    private var currentUserId: String? = null // 로그인한 사용자 ID

    // 🔹 admin 가상의 좌표값 (초기값: 서울)
    private var simulatedLocation = LatLng(37.5665, 126.9780)
    private var pendingLatOffset = 0.0
    private var pendingLngOffset = 0.0

    // 🔹 지도 초기 축척 설정
    private var initialZoomLevel: Float = 15f

    // 🔹 0.5초마다 위치 갱신을 위한 핸들러
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

            updatedLocation?.let { updateRandomCircles(it) }

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
                if (currentUserId != "admin") { // 🔹 Admin이면 GPS 데이터 무시
                    locationResult.lastLocation?.let { location ->
                        updateMapLocation(location)
                    }
                }
            }
        }


        currentUserId = intent.getStringExtra("USER_ID")

        // 🔹 0.5초마다 위치 업데이트 시작
        locationUpdateHandler.post(locationUpdateRunnable)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(simulatedLocation, initialZoomLevel)) // 초깃값 적용
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
        googleMap?.animateCamera(CameraUpdateFactory.newLatLng(myLatLng)) // 초점만 이동, 축척 유지

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
        googleMap?.animateCamera(CameraUpdateFactory.newLatLng(simulatedLocation)) // 초점만 이동, 축척 유지

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
                KeyEvent.KEYCODE_DPAD_UP -> pendingLatOffset += 0.00005
                KeyEvent.KEYCODE_DPAD_DOWN -> pendingLatOffset -= 0.00005
                KeyEvent.KEYCODE_DPAD_LEFT -> pendingLngOffset -= 0.00005
                KeyEvent.KEYCODE_DPAD_RIGHT -> pendingLngOffset += 0.00005
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun requestLocationUpdate() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    updateMapLocation(location)
                }
            }
        }
    }

    private fun updateRandomCircles(center: LatLng) {
        // 🔹 기존 파란색 원 삭제
        randomCircles.forEach { it.remove() }
        randomCircles.clear()

        // 🔹 새 파란색 원 3개 생성
        repeat(3) {
            val randomOffsetLat = (Random.nextDouble(-0.0009, 0.0009)) // 약 100m 이내
            val randomOffsetLng = (Random.nextDouble(-0.0009, 0.0009))
            val randomLatLng = LatLng(center.latitude + randomOffsetLat, center.longitude + randomOffsetLng)

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
