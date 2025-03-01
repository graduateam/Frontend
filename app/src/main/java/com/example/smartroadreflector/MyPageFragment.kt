package com.example.smartroadreflector

import android.content.Context
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

        btnLogout.setOnClickListener {
            // 로그아웃 로직 추가
        }

        btnChangePassword.setOnClickListener {
            // 비밀번호 변경 로직 추가
        }

        btnDeleteAccount.setOnClickListener {
            // 회원탈퇴 로직 추가
        }

        return view
    }

    // 🔹 로그인한 사용자 닉네임 가져오는 함수
    private fun getLoggedInUserNickname(context: Context): String? {
        val sharedPrefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        return sharedPrefs.getString("LOGGED_IN_NICKNAME", "사용자")
    }

}
