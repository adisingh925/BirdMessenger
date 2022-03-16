package com.adreal.birdmessenger.Activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.adreal.birdmessenger.R

class License : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_BirdMessenger)
        setContentView(R.layout.activity_license)
    }
}