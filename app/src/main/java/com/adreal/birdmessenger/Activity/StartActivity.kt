package com.adreal.birdmessenger.Activity

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.adreal.birdmessenger.Constants.Constants
import com.adreal.birdmessenger.Constants.Constants.NO
import com.adreal.birdmessenger.Constants.Constants.YES
import com.adreal.birdmessenger.Encryption.Encryption
import com.adreal.birdmessenger.R
import com.adreal.birdmessenger.SharedPreferences.SharedPreferences
import com.adreal.birdmessenger.ViewModel.StartActivityViewModel
import com.adreal.birdmessenger.databinding.ActivityStartBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
        changeCurrentState(YES)
        super.onResume()
    }

    override fun onPause() {
        changeCurrentState(NO)
        super.onPause()
    }

    private fun changeCurrentState(state : String){
        SharedPreferences.write(Constants.ON_FOREGROUND,state)
        if(state == NO){
            startActivityViewModel.setStatus(0)
        }else{
            startActivityViewModel.setStatus(1)
        }
    }
}