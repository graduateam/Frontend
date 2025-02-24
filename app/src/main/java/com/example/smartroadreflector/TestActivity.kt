package com.example.smartroadreflector

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.smartroadreflector.databinding.ActivityLoginBinding

class TestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}