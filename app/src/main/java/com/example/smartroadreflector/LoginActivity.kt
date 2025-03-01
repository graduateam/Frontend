package com.example.smartroadreflector

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PorterDuff
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.smartroadreflector.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var dbHelper: UserDatabaseHelper
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = UserDatabaseHelper(this)
        sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        // 자동 로그인 상태 확인 후 실행
        if (isAutoLoginEnabled()) {
            autoLogin()
        }

        // UI 초기화 (체크박스 색상)
        updateAutoLoginUI(isAutoLoginEnabled())

        // 자동 로그인 버튼 클릭 이벤트
        binding.autoLoginButton.setOnClickListener {
            toggleAutoLogin()
        }

        // 뒤로 가기 버튼
        binding.loginButtonBack.setOnClickListener {
            startActivity(Intent(this, StartActivity::class.java))
            finish()
        }

        // 로그인 버튼
        binding.loginSubmitButton.setOnClickListener {
            val username = binding.editTextUsername.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "아이디와 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (dbHelper.loginUser(username, password)) {
                // 자동 로그인 설정 시 아이디/비밀번호 저장
                sharedPreferences.edit()
                    .putString("LAST_USERNAME", username)
                    .putString("LAST_PASSWORD", password)
                    .apply()

                Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "로그인 실패: 아이디 또는 비밀번호가 틀렸습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 자동 로그인 상태 변경
    private fun toggleAutoLogin() {
        val isEnabled = isAutoLoginEnabled()
        setAutoLoginEnabled(!isEnabled)
        updateAutoLoginUI(!isEnabled)
    }

    // 자동 로그인 여부 확인
    private fun isAutoLoginEnabled(): Boolean {
        return sharedPreferences.getBoolean("AUTO_LOGIN", false)
    }

    // 자동 로그인 설정 변경
    private fun setAutoLoginEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("AUTO_LOGIN", enabled).apply()
    }

    // UI 업데이트 (색상 변경)
    private fun updateAutoLoginUI(isEnabled: Boolean) {
        val checkBox = binding.checkBox
        if (isEnabled) {
            checkBox.setColorFilter(ContextCompat.getColor(this, R.color.green), PorterDuff.Mode.SRC_IN)
        } else {
            checkBox.clearColorFilter() // 기본 색상 복원
        }
    }

    // 자동 로그인 실행
    private fun autoLogin() {
        val lastUsername = sharedPreferences.getString("LAST_USERNAME", "") ?: ""
        val lastPassword = sharedPreferences.getString("LAST_PASSWORD", "") ?: ""

        if (lastUsername.isNotEmpty() && lastPassword.isNotEmpty()) {
            if (dbHelper.loginUser(lastUsername, lastPassword)) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}
