package com.example.smartroadreflector

import android.content.Context
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

            val currentPassword = editCurrentPassword.text.toString()
            val newPassword = editNewPassword.text.toString()
            val confirmPassword = editConfirmPassword.text.toString()

            if (username == null) {
                Toast.makeText(this, "로그인 정보가 없습니다. 다시 로그인하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "새 비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!dbHelper.loginUser(username, currentPassword)) {
                Toast.makeText(this, "현재 비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 비밀번호 업데이트
            if (dbHelper.updatePassword(username, newPassword)) {
                Toast.makeText(this, "비밀번호가 변경되었습니다.", Toast.LENGTH_SHORT).show()
                finish() // 변경 완료 후 현재 화면 닫기
            } else {
                Toast.makeText(this, "비밀번호 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
