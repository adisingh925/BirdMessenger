package com.adreal.birdmessenger.Activity

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.adreal.birdmessenger.Encryption.Encryption
import com.adreal.birdmessenger.R
import com.adreal.birdmessenger.SharedPreferences.SharedPreferences
import com.adreal.birdmessenger.ViewModel.StartActivityViewModel
import com.adreal.birdmessenger.databinding.ActivityStartBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class StartActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityStartBinding.inflate(layoutInflater)
    }

    val startActivityViewModel by lazy {
        ViewModelProvider(this)[StartActivityViewModel::class.java]
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_BirdMessenger)
        setContentView(binding.root)

        SharedPreferences.init(this)

        CoroutineScope(Dispatchers.IO).launch {
            startActivityViewModel.saveInstallationId()
            startActivityViewModel.saveToken()
            startActivityViewModel.subscribeToCommonTopic()
            startActivityViewModel.subscribeToIndividualTopic()
            startActivityViewModel.uploadToken()
//            startActivityViewModel.storeAESPublicKey()
            startActivityViewModel.storeDHKeyPair()
        }
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