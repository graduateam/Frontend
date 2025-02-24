package com.example.smartroadreflector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class MyPageFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_my_page, container, false)

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
}
