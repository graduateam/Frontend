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
import com.google.android.gms.maps.model.PolygonOptions
import kotlin.random.Random

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var userLocationCircle: Circle? = null
    private var randomCircles: MutableList<Circle> = mutableListOf()
    private var isFirstLocationUpdate = true // 📌 최초 위치 업데이트 여부 확인
    private var receiveCount = 0 // 📌 위치 업데이트 횟수 카운트
    private var currentUserId: String? = null // 📌 현재 로그인한 사용자 ID

    // 📌 관리자 위치 시뮬레이션 변수
    private var simulatedLocation = LatLng(37.67677149273746, 126.74587662812687)
    private var pendingLatOffset = 0.0
    private var pendingLngOffset = 0.0

    private var initialZoomLevel: Float = 20f // 📌 초기 줌 레벨 설정

    private val locationUpdateHandler = Handler(Looper.getMainLooper())

    // 📌 0.5초마다 위치 업데이트를 수행하는 Runnable
    private val locationUpdateRunnable = object : Runnable {
        override fun run() {
            val updatedLocation = if (currentUserId == "admin") {
                applyPendingAdminMovement()
                simulatedLocation
            } else {
                requestLocationUpdate()
                userLocationCircle?.center // 현재 GPS 위치
            }

            updatedLocation?.let { updateRandomCircles(it) } // 🔹 랜덤한 원(circle) 위치 갱신

            locationUpdateHandler.postDelayed(this, 500) // 0.5초마다 실행
        }
    }

    // 📌 플롯 데이터를 저장할 데이터 클래스 추가
    data class PlotData(
        val id: Int,            // 객체 ID
        val type: PlotType,     // 객체 종류 (차량/인원)
        val position: LatLng,   // 위치 (위도, 경도)
        val direction: Float,   // 방향 (각도)
        val speed: Float        // 속도 (m/s)
    )

    // 📌 차량/인원 타입을 정의하는 enum 추가
    enum class PlotType {
        VEHICLE, PERSON
    }

    // 📌 플롯 관리 변수 추가
    private var plotIdCounter = 1
    private var isNextPlotVehicle = true // 번갈아가며 차량/인원 추가 여부
    private val plotMarkers = mutableListOf<Any>() // 마커 또는 도형 저장 리스트

    // 📌 플롯 크기 변수 (필요에 따라 변경 가능!)
    private var vehicleSize = 0.00002  // 차량 크기 (위도/경도 단위, 기본값 약 2.2m)
    private var personRadius = 0.8    // 인원 크기 (반경, 기본값 0.8m)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val drawerLayout: DrawerLayout = binding.drawerLayout

        // 📌 마이페이지 및 설정 페이지 프래그먼트 추가
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_my_page, MyPageFragment())
            .commit()

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_settings, SettingsFragment())
            .commit()

        // 📌 버튼 클릭 시 드로어 열기
        binding.mainUserButton.setOnClickListener {
            drawerLayout.openDrawer(binding.fragmentContainerMyPage)
        }

        binding.mainSettingButton.setOnClickListener {
            drawerLayout.openDrawer(binding.fragmentContainerSettings)
        }

        // 📌 지도 프래그먼트 가져오기 및 초기화
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapView2) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        // 📌 위치 서비스 초기화
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

        // 📌 위치 업데이트 시작
        locationUpdateHandler.post(locationUpdateRunnable)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // 📌 초기 지도 설정
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(simulatedLocation, initialZoomLevel))
        startLocationUpdates()

        // 📌 지도 터치 이벤트 리스너 추가
        googleMap?.setOnMapClickListener { latLng ->
            addPlot(latLng)
        }
    }

    // 📌 GPS 위치 업데이트 요청 함수
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

    // 📌 현재 위치를 지도에 반영하는 함수
    private fun updateMapLocation(location: Location) {
        val myLatLng = LatLng(location.latitude, location.longitude)

        if (isFirstLocationUpdate) {
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, initialZoomLevel))
            isFirstLocationUpdate = false
        }

        userLocationCircle?.remove()
        userLocationCircle = googleMap?.addCircle(
            CircleOptions()
                .center(myLatLng)
                .radius(2.0)
                .strokeColor(0xFFFF0000.toInt())
                .fillColor(0x33FF0000.toInt())
        )

        receiveCount++
    }

    // 📌 관리자 모드에서 이동 오프셋 적용 함수
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

    // 📌 관리자 위치를 지도에 갱신하는 함수
    private fun updateAdminMapLocation() {
        userLocationCircle?.remove()
        userLocationCircle = googleMap?.addCircle(
            CircleOptions()
                .center(simulatedLocation)
                .radius(2.0)
                .strokeColor(0xFFFF0000.toInt())
                .fillColor(0x33FF0000.toInt())
        )
    }

    // 📌 랜덤한 원을 지도에 추가하는 함수
    private fun updateRandomCircles(center: LatLng) {
        randomCircles.forEach { it.remove() }
        randomCircles.clear()

        repeat(3) {
            val randomLatLng = LatLng(
                center.latitude + Random.nextDouble(-0.00009, 0.00009),
                center.longitude + Random.nextDouble(-0.00009, 0.00009)
            )

            val circle = googleMap?.addCircle(
                CircleOptions()
                    .center(randomLatLng)
                    .radius(1.0)
                    .strokeColor(0xFF0000FF.toInt())
                    .fillColor(0x330000FF.toInt())
            )
            circle?.let { randomCircles.add(it) }
        }
    }

    private fun addPlot(latLng: LatLng) {
        val plotType = if (isNextPlotVehicle) PlotType.VEHICLE else PlotType.PERSON
        val direction = Random.nextFloat() * 360
        val speed = Random.nextFloat() * 10

        val plotData = PlotData(plotIdCounter++, plotType, latLng, direction, speed)

        when (plotType) {
            PlotType.VEHICLE -> addVehiclePlot(plotData)
            PlotType.PERSON -> addPersonPlot(plotData)
        }

        isNextPlotVehicle = !isNextPlotVehicle
    }

    // 📌 차량 플롯(사각형) 추가 함수 (Polygon 사용)
    private fun addVehiclePlot(plotData: PlotData) {
        val lat = plotData.position.latitude
        val lng = plotData.position.longitude

        val corners = listOf(
            LatLng(lat + vehicleSize, lng - vehicleSize),
            LatLng(lat + vehicleSize, lng + vehicleSize),
            LatLng(lat - vehicleSize, lng + vehicleSize),
            LatLng(lat - vehicleSize, lng - vehicleSize)
        )

        val polygon = googleMap?.addPolygon(
            PolygonOptions()
                .addAll(corners)
                .strokeColor(0xFF0000FF.toInt())
                .fillColor(0x330000FF.toInt())
                .strokeWidth(5f)
        )

        polygon?.let { plotMarkers.add(it) }
    }


    // 📌 인원 플롯(원) 추가 함수
    private fun addPersonPlot(plotData: PlotData) {
        val circle = googleMap?.addCircle(
            CircleOptions()
                .center(plotData.position)
                .radius(personRadius)
                .strokeColor(0xFFFF0000.toInt())
                .fillColor(0x33FF0000.toInt())
        )

        circle?.let { plotMarkers.add(it) }
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
