package com.example.smartroadreflector

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class MyPageFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_my_page, container, false)

        // 닉네임 가져와서 txt_greeting 업데이트
        val txtGreeting = view.findViewById<TextView>(R.id.txt_greeting)
        val nickname = getLoggedInUserNickname(requireContext()) ?: "사용자"
        txtGreeting.text = "반가워요 ${nickname}님,\n오늘도 안전운전~"

        // 버튼 클릭 리스너 설정
        val btnLogout = view.findViewById<Button>(R.id.btn_logout)
        val btnChangePassword = view.findViewById<Button>(R.id.btn_change_password)
        val btnDeleteAccount = view.findViewById<Button>(R.id.btn_delete_account)

        // 로그아웃 버튼 클릭 이벤트
        btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        return view
    }

    // 🔹 로그인한 사용자 닉네임 가져오는 함수
    private fun getLoggedInUserNickname(context: Context): String? {
        val sharedPrefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        return sharedPrefs.getString("LOGGED_IN_NICKNAME", "사용자")
    }

    // 🔹 로그아웃 확인 팝업 표시
    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("로그아웃")
            .setMessage("로그아웃 하시겠습니까?")
            .setPositiveButton("예") { _, _ ->
                logoutUser()
            }
            .setNegativeButton("아니오", null)
            .show()
    }

    // 🔹 로그아웃 처리 및 데이터 초기화
    private fun logoutUser() {
        val sharedPrefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            remove("LAST_USERNAME")  // 자동 로그인 정보 삭제
            remove("LAST_PASSWORD")
            remove("LOGGED_IN_NICKNAME")
            apply()
        }

        // 로그인 화면으로 이동
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
