package com.example.smartroadreflector

// 최소 8자 이상
// 영문 + 숫자 + 특수문자( @#!%*?& ) 포함

object PasswordUtils {
    fun isValidPassword(password: String): Boolean {
        val passwordPattern = Regex("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}$")
        return passwordPattern.matches(password)
    }
}
