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
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import java.util.*

@RequiresApi(Build.VERSION_CODES.P)
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

        CoroutineScope(Dispatchers.IO).launch {
            Encryption().addBouncyCastleProvider()
            startActivityViewModel.saveInstallationId()
            startActivityViewModel.saveToken()
            startActivityViewModel.subscribeToCommonTopic()
            startActivityViewModel.subscribeToIndividualTopic()
            startActivityViewModel.uploadToken()
            startActivityViewModel.storeDHKeyPair()
            startActivityViewModel.storeECDHKeyPair()
        }
    }

    override fun onResume() {
        startActivityViewModel.dismissNotifications(this@StartActivity)
        SharedPreferences.write("onForeground","y")
        startActivityViewModel.setStatus(1)
        super.onResume()
    }

    override fun onPause() {
        SharedPreferences.write("onForeground","n")
        startActivityViewModel.setStatus(0)
        super.onPause()
    }
}