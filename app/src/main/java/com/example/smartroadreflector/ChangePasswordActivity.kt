package com.example.smartroadreflector

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
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
        val btnBack = findViewById<ImageButton>(R.id.change_password_button_back)

        // 🔹 뒤로 가기 버튼 클릭 시 이전 화면으로 돌아가기
        btnBack.setOnClickListener {
            finish() // 현재 액티비티 종료
        }

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

            if (!isValidPassword(newPassword)) {
                Toast.makeText(this, "비밀번호는 8자 이상, 영문 + 숫자 + 특수문자를 포함해야 합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔹 비밀번호 업데이트
            if (dbHelper.updatePassword(username, newPassword)) {
                Toast.makeText(this, "비밀번호가 변경되었습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show()
                logoutUser()
            } else {
                Toast.makeText(this, "비밀번호 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 🔹 EditText 외의 화면 터치 시 키보드 숨기기
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            val view = currentFocus
            if (view is EditText) {
                hideKeyboard(view)
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    // 🔹 비밀번호 유효성 검사 함수
    private fun isValidPassword(password: String): Boolean {
        val passwordPattern = Regex("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}$")
        return passwordPattern.matches(password)
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
