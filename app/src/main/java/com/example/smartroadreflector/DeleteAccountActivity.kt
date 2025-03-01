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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class DeleteAccountActivity : AppCompatActivity() {

    private lateinit var dbHelper: UserDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delete_account)

        dbHelper = UserDatabaseHelper(this)

        val editPassword = findViewById<EditText>(R.id.edit_delete_password)
        val btnDeleteAccount = findViewById<Button>(R.id.btn_confirm_delete)
        val btnBack = findViewById<ImageButton>(R.id.delete_account_button_back)

        // 🔹 뒤로 가기 버튼 클릭 시 이전 화면으로 이동
        btnBack.setOnClickListener {
            finish()
        }

        btnDeleteAccount.setOnClickListener {
            val sharedPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val username = sharedPrefs.getString("LAST_USERNAME", null)

            val password = editPassword.text.toString().trim()

            if (username == null) {
                Toast.makeText(this, "로그인 정보가 없습니다. 다시 로그인하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                Toast.makeText(this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔹 비밀번호 확인 후 삭제
            if (dbHelper.loginUser(username, password)) {
                showDeleteConfirmationDialog(username)
            } else {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 🔹 회원 탈퇴 확인 다이얼로그 표시
    private fun showDeleteConfirmationDialog(username: String) {
        AlertDialog.Builder(this)
            .setTitle("회원 탈퇴")
            .setMessage("정말로 계정을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.")
            .setPositiveButton("예") { _, _ ->
                deleteAccount(username)
            }
            .setNegativeButton("아니오", null)
            .show()
    }

    // 🔹 회원 탈퇴 처리 및 로그아웃
    private fun deleteAccount(username: String) {
        if (dbHelper.deleteUserAccount(username)) {
            // 🔹 로그인 정보 삭제
            val sharedPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            with(sharedPrefs.edit()) {
                remove("LAST_USERNAME")
                remove("LAST_PASSWORD")
                remove("LOGGED_IN_NICKNAME")
                apply()
            }

            Toast.makeText(this, "계정이 삭제되었습니다.", Toast.LENGTH_SHORT).show()

            // 시작 화면으로 이동
            val intent = Intent(this, StartActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "계정 삭제에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
        }
    }

    // 🔹 화면 터치 시 키보드 숨기기 (LoginActivity, RegisterActivity와 동일)
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
}
