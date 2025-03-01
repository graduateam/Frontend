package com.example.smartroadreflector

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartroadreflector.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var dbHelper: UserDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = UserDatabaseHelper(this)

        binding.registerButtonBack.setOnClickListener {
            startActivity(Intent(this, StartActivity::class.java))
            finish()
        }

        binding.registerSubmitButton.setOnClickListener {
            val username = binding.editTextUsername.text.toString().trim()
            val nickname = binding.editTextNickname.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()
            val confirmPassword = binding.editTextPasswordConfirm.text.toString().trim()
            val email = binding.editTextEmail.text.toString().trim()

            if (username.isEmpty() || nickname.isEmpty() || password.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "유효한 이메일을 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔹 비밀번호 유효성 검사 추가
            if (!isValidPassword(password)) {
                Toast.makeText(
                    this,
                    "비밀번호는 8자 이상이며, 영문 + 숫자 + 특수문자를 포함해야 합니다.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val success = dbHelper.registerUser(username, nickname, password, email)
            if (success) {
                // 🔹 회원가입 성공 후 자동 로그인 정보 저장
                val sharedPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                with(sharedPrefs.edit()) {
                    putString("LAST_USERNAME", username)
                    putString("LOGGED_IN_NICKNAME", nickname)
                    apply()
                }

                Toast.makeText(this, "회원가입 성공! 자동 로그인됩니다.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, WelcomeActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "회원가입 실패: 중복된 아이디 또는 이메일이 존재합니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 화면을 터치하면 키보드를 숨기도록 설정
        binding.root.setOnTouchListener { view, _ ->
            hideKeyboard()
            view.performClick() // performClick() 호출하여 경고 해결
            false
        }
    }

    // 🔹 비밀번호 유효성 검사 함수
    private fun isValidPassword(password: String): Boolean {
        val passwordPattern = Regex("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}$")
        return passwordPattern.matches(password)
    }

    // 키보드를 숨기는 함수
    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }
}
