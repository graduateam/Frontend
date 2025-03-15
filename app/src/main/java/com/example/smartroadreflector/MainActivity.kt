package com.example.smartroadreflector

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.smartroadreflector.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
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
    private var locationUpdateHandler: Handler = Handler(Looper.getMainLooper())
    private var userLocationCircle: Circle? = null
    private var isFirstLocationUpdate = true // 🔹 최초 위치 업데이트 여부 체크

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val drawerLayout: DrawerLayout = binding.drawerLayout

        // 마이페이지 프래그먼트 추가
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_my_page, MyPageFragment())
            .commit()

        // 환경설정 프래그먼트 추가
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_settings, SettingsFragment())
            .commit()

        // 버튼 클릭 시 드로어 열기
        binding.mainUserButton.setOnClickListener {
            drawerLayout.openDrawer(binding.fragmentContainerMyPage)
        }

        binding.mainSettingButton.setOnClickListener {
            drawerLayout.openDrawer(binding.fragmentContainerSettings)
        }

        // 🔹 SupportMapFragment 초기화 및 설정
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapView2) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        // 🔹 현재 위치 제공자 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // 🔹 1초마다 위치 업데이트 시작
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        locationUpdateHandler.post(object : Runnable {
            override fun run() {
                getMyLocation()
                locationUpdateHandler.postDelayed(this, 1000) // 1초마다 실행
            }
        })
    }

    private fun getMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location == null) {
                // 🚨 위치가 null이면 로그 출력
                android.util.Log.e("MainActivity", "🚨 GPS 데이터를 받아오지 못했습니다. 기본 위치(서울)를 사용합니다.")
            }

            val myLatLng = location?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(37.5665, 126.9780) // 기본 위치: 서울

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

            android.util.Log.d("MainActivity", "🗺️ 현재 위치 업데이트: ${myLatLng.latitude}, ${myLatLng.longitude}")
        }.addOnFailureListener {
            android.util.Log.e("MainActivity", "🚨 위치 요청 실패: ${it.message}")
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getMyLocation()
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
