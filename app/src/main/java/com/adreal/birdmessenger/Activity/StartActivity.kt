package com.adreal.birdmessenger.Activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.adreal.birdmessenger.Encryption.Encryption
import com.adreal.birdmessenger.R
import com.adreal.birdmessenger.SharedPreferences.SharedPreferences
import com.adreal.birdmessenger.ViewModel.StartActivityViewModel
import com.adreal.birdmessenger.databinding.ActivityStartBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StartActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityStartBinding.inflate(layoutInflater)
    }

    val startActivityViewModel by lazy {
        ViewModelProvider(this)[StartActivityViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_BirdMessenger)
        setContentView(binding.root)

        SharedPreferences.init(this)
        startActivityViewModel.saveInstallationId()
        startActivityViewModel.saveToken()
        startActivityViewModel.subscribeToCommonTopic()
        startActivityViewModel.subscribeToIndividualTopic()
        startActivityViewModel.uploadToken()
        val key = Encryption().generateKeyPair()
        Log.d("keys",key.private.toString() + " " + key.public)
    }

    override fun onResume() {
        startActivityViewModel.setStatus(1)
        super.onResume()
    }

    override fun onPause() {
        startActivityViewModel.setStatus(0)
        super.onPause()
    }
}