package com.adreal.birdmessenger.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.liveData
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.adreal.birdmessenger.Database.Database
import com.adreal.birdmessenger.Model.ChatModel
import com.adreal.birdmessenger.Model.UserModel
import com.adreal.birdmessenger.Repository.Repository
import com.adreal.birdmessenger.Worker.UploadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class OfflineViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: Repository

    val readAllUsers: LiveData<List<UserModel>>

    private val imageLiveData = MutableLiveData<String>()

    init {
        val dao = Database.getDatabase(application).Dao()
        repository = Repository(dao)
        readAllUsers = repository.readAllUsers
    }

    fun readAllMessages(senderId: String, receiverId: String): LiveData<PagingData<ChatModel>> {
        return Pager(
            PagingConfig(20, enablePlaceholders = false),
            pagingSourceFactory = repository.readAllMessages(senderId, receiverId)
                .asPagingSourceFactory(Dispatchers.IO)
        ).liveData
    }

    fun addChatData(data: ChatModel) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addChatData(data)
        }
    }

    fun addNewUser(data: UserModel) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addNewUser(data)
        }
    }

    fun updateChatData(data: ChatModel) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateChatData(data)
        }
    }

    fun updateUserCardData(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            Database.getDatabase(getApplication()).Dao().updateUserCardData(userId)
        }
    }

    fun initUserImage(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            imageLiveData.postValue(
                Database.getDatabase(getApplication()).Dao().getImageData(userId)
            )
        }
    }

    fun setupWorkManager() {
        viewModelScope.launch(Dispatchers.IO) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(false)
                .build()

            val uploadWorkRequest: WorkRequest =
                OneTimeWorkRequestBuilder<UploadWorker>().setConstraints(constraints).build()

            WorkManager
                .getInstance(getApplication())
                .enqueue(uploadWorkRequest)
        }
    }
}