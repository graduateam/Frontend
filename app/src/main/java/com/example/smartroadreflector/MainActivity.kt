package com.example.smartroadreflector

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
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
    private var debugTextViews: List<TextView>? = null

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
                locationResult.lastLocation?.let { location ->
                    updateMapLocation(location)
                }
            }
        }

        // 🔹 로그인한 사용자 ID 가져오기
        currentUserId = intent.getStringExtra("USER_ID")

        // 🔹 admin 계정이면 디버그 UI 추가
        if (currentUserId == "admin") {
            showDebugInfo()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
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

        if (currentUserId == "admin") {
            updateDebugInfo(location)
        }

        android.util.Log.d("MainActivity", "🗺️ 현재 위치 업데이트: ${myLatLng.latitude}, ${myLatLng.longitude}")
    }

    private fun showDebugInfo() {
        val rootLayout = findViewById<FrameLayout>(android.R.id.content)

        val debugLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(0xAAFFFFFF.toInt()) // 반투명 흰색 배경
            gravity = Gravity.CENTER
        }

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            setMargins(20, 100, 20, 0)
        }

        val debugIdText = TextView(this).apply {
            textSize = 16f
            setTextColor(android.graphics.Color.BLACK)
            text = "ID: $currentUserId"
        }

        val debugCountText = TextView(this).apply {
            textSize = 16f
            setTextColor(android.graphics.Color.BLACK)
            text = "수신 횟수: 0"
        }

        val debugLatText = TextView(this).apply {
            textSize = 16f
            setTextColor(android.graphics.Color.BLACK)
            text = "위도: --"
        }

        val debugLonText = TextView(this).apply {
            textSize = 16f
            setTextColor(android.graphics.Color.BLACK)
            text = "경도: --"
        }

        debugLayout.addView(debugIdText)
        debugLayout.addView(debugCountText)
        debugLayout.addView(debugLatText)
        debugLayout.addView(debugLonText)

        rootLayout.addView(debugLayout, params)

        debugTextViews = listOf(debugIdText, debugCountText, debugLatText, debugLonText)
    }

    private fun updateDebugInfo(location: Location) {
        debugTextViews?.let { texts ->
            texts[1].text = "수신 횟수: $receiveCount"
            texts[2].text = "위도: ${location.latitude}"
            texts[3].text = "경도: ${location.longitude}"
        }
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
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
