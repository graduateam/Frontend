package com.example.smartroadreflector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val checkVibration = view.findViewById<CheckBox>(R.id.check_vibration)
        val checkVoice = view.findViewById<CheckBox>(R.id.check_voice)

        return view
    }
}
