package com.example.smartroadreflector

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.example.smartroadreflector.databinding.ActivityMainBinding
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.CircleOverlay
import com.naver.maps.map.overlay.GroundOverlay
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PathOverlay
import com.naver.maps.map.overlay.PolygonOverlay

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    // 줌 레벨 임계값 (예: 15.0 이상이면 위성지도)
    private val zoomThreshold = 15.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val drawerLayout: DrawerLayout = binding.drawerLayout

        // 마이페이지 및 설정 페이지 프래그먼트 추가
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_my_page, MyPageFragment())
            .commit()

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

        // 네이버 지도 프래그먼트 초기화 (XML에 정의된 map_fragment 사용)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as? MapFragment
            ?: MapFragment.newInstance().also {
                supportFragmentManager.beginTransaction().add(R.id.map_fragment, it).commit()
            }
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(naverMap: NaverMap) {
        // 서울 중심 좌표
        val seoulCenter = LatLng(37.5665, 126.9780)
        naverMap.moveCamera(CameraUpdate.scrollTo(seoulCenter))
        naverMap.moveCamera(CameraUpdate.zoomTo(13.0))

        // 카메라 이동이 멈출 때마다 줌 레벨에 따라 지도 타입 변경
        naverMap.addOnCameraIdleListener {
            val currentZoom = naverMap.cameraPosition.zoom
            if (currentZoom >= zoomThreshold) {
                if (naverMap.mapType != NaverMap.MapType.Satellite) {
                    naverMap.mapType = NaverMap.MapType.Satellite
                }
            } else {
                if (naverMap.mapType != NaverMap.MapType.Basic) {
                    naverMap.mapType = NaverMap.MapType.Basic
                }
            }
        }

        // 오프셋 값 (좌표 단위, 약 0.005도 정도 → 약 500m 내외)
        val offset = 0.005

        // 1. Marker (서울 중심)
        val marker = Marker().apply {
            position = seoulCenter
            captionText = "Marker"
        }
        marker.map = naverMap

        // 2. CircleOverlay (첫 번째 오버레이: 원)
        val circleOverlay = CircleOverlay().apply {
            center = LatLng(seoulCenter.latitude, seoulCenter.longitude + offset)
            radius = 200.0  // 반경 200m
            color = 0x550000FF  // 반투명 파란색 (채우기 색상)
            outlineColor = 0xFF0000FF.toInt()  // 진한 파란색 (외곽선)
            outlineWidth = 4
        }
        circleOverlay.map = naverMap

        // 3. PolygonOverlay (두 번째 오버레이: 다각형)
        val polygonCenter = LatLng(seoulCenter.latitude, seoulCenter.longitude + 2 * offset)
        val halfSide = 0.002  // 다각형의 크기 조절 (약 200m 정도)
        val polygonOverlay = PolygonOverlay().apply {
            coords = listOf(
                LatLng(polygonCenter.latitude - halfSide, polygonCenter.longitude - halfSide),
                LatLng(polygonCenter.latitude - halfSide, polygonCenter.longitude + halfSide),
                LatLng(polygonCenter.latitude + halfSide, polygonCenter.longitude + halfSide),
                LatLng(polygonCenter.latitude + halfSide, polygonCenter.longitude - halfSide)
            )
            color = 0x5500FF00  // 반투명 녹색
            outlineColor = 0xFF00FF00.toInt()  // 진한 녹색
            outlineWidth = 4
        }
        polygonOverlay.map = naverMap

        // 4. PathOverlay (세 번째 오버레이: 경로)
        val pathOverlay = PathOverlay().apply {
            coords = listOf(
                LatLng(seoulCenter.latitude, seoulCenter.longitude + 3 * offset),
                LatLng(seoulCenter.latitude, seoulCenter.longitude + 3.5 * offset),
                LatLng(seoulCenter.latitude, seoulCenter.longitude + 4 * offset)
            )
            color = 0xFF888888.toInt()  // 회색
            outlineColor = 0xFF000000.toInt()  // 검정
            outlineWidth = 6
        }
        pathOverlay.map = naverMap

        // 5. GroundOverlay (네 번째 오버레이: 지면 오버레이)
        val groundCenter = LatLng(seoulCenter.latitude, seoulCenter.longitude + 5 * offset)
        val groundOverlay = GroundOverlay().apply {
            image = OverlayImage.fromResource(R.drawable.ic_launcher_foreground)
            bounds = LatLngBounds(
                LatLng(groundCenter.latitude - 0.001, groundCenter.longitude - 0.001),
                LatLng(groundCenter.latitude + 0.001, groundCenter.longitude + 0.001)
            )
            alpha = 0.8f
        }
        groundOverlay.map = naverMap
    }
}
