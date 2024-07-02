package com.example.vertex.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vertex.data.models.Configuration
import com.example.vertex.data.models.UserResponse
import com.example.vertex.data.remote.RetrofitInstance
import dagger.hilt.android.internal.Contexts.getApplication
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SharedViewModel() : ViewModel() {

    private val _configuration = MutableLiveData<Result<Configuration>>()
    val configuration: LiveData<Result<Configuration>> get() = _configuration

    private val _data = MutableLiveData<Result<UserResponse>>()
    val data: LiveData<Result<UserResponse>> get() = _data


    fun fetchConfiguration() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                RetrofitInstance.api.getConfiguration()
            }
            _configuration.postValue(result)
        }
    }

    fun fetchUserData(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                RetrofitInstance.api.getFormResponse(url)
            }
            _data.postValue(result)
        }
    }
}

//class SharedViewModel(private val repository: Repository) : ViewModel() {
//
//    private val _configuration = MutableLiveData<Result<Configuration>>()
//    val configuration: LiveData<Result<Configuration>> get() = _configuration
//
//    private val _data = MutableLiveData<Result<UserResponse>>()
//    val data: LiveData<Result<UserResponse>> get() = _data
//
//    fun fetchConfiguration() {
//        viewModelScope.launch(Dispatchers.IO) {
//            val result = repository.getConfiguration()
//            _configuration.postValue(result)
//        }
//    }
//
//    fun fetchUserData(url: String) {
//        viewModelScope.launch(Dispatchers.IO) {
//            val result = repository.getUserData(url)
//            _data.postValue(result)
//        }
//    }
//}