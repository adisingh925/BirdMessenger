package com.adreal.birdmessenger.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.adreal.birdmessenger.Database.Database
import com.adreal.birdmessenger.Model.VideoCallModel

class VideoCallViewModel(application: Application) : AndroidViewModel(application) {

    val readAllCalls : LiveData<List<VideoCallModel>>

    init {
        readAllCalls = Database.getDatabase(application).Dao().getAllCalls()
    }
}