package com.adreal.birdmessenger.Activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.adreal.birdmessenger.R
import com.adreal.birdmessenger.databinding.ActivityInfoBinding

class Info : AppCompatActivity() {

    lateinit var binding: ActivityInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.textview2.setOnClickListener()
        {
            val intent = Intent(this,License::class.java)
            startActivity(intent)
        }
    }
}