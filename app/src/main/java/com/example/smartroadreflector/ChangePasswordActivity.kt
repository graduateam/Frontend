package com.example.smartroadreflector

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var dbHelper: UserDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        dbHelper = UserDatabaseHelper(this)

        val editCurrentPassword = findViewById<EditText>(R.id.edit_current_password)
        val editNewPassword = findViewById<EditText>(R.id.edit_new_password)
        val editConfirmPassword = findViewById<EditText>(R.id.edit_confirm_password)
        val btnChangePassword = findViewById<Button>(R.id.btn_submit_new_password)

        btnChangePassword.setOnClickListener {
            val sharedPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val username = sharedPrefs.getString("LAST_USERNAME", null)

            val currentPassword = editCurrentPassword.text.toString().trim()
            val newPassword = editNewPassword.text.toString().trim()
            val confirmPassword = editConfirmPassword.text.toString().trim()

            if (username == null) {
                Toast.makeText(this, "로그인 정보가 없습니다. 다시 로그인하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!dbHelper.loginUser(username, currentPassword)) {
                Toast.makeText(this, "현재 비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "새 비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!PasswordUtils.isValidPassword(newPassword)) {
                Toast.makeText(this, "비밀번호는 8자 이상, 영문+숫자+특수문자를 포함해야 합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 비밀번호 업데이트
            if (dbHelper.updatePassword(username, newPassword)) {
                Toast.makeText(this, "비밀번호가 변경되었습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show()

                // 자동 로그아웃 처리
                logoutUser()
            } else {
                Toast.makeText(this, "비밀번호 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 🔹 로그아웃 처리 및 데이터 초기화
    private fun logoutUser() {
        val sharedPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            remove("LAST_USERNAME")
            remove("LAST_PASSWORD")
            remove("LOGGED_IN_NICKNAME")
            apply()
        }

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
