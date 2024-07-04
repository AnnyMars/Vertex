package com.example.vertex.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

class SystemStateReceiver(
    private val onBatteryChanged: (Int) -> Unit,
    private val onWifiChanged: (Int) -> Unit,
    private val onConnectionChange: (Boolean) -> Unit
) : BroadcastReceiver() {

    private var wasConnected: Boolean? = false

    override fun onReceive(context: Context, intent: Intent?) {

        when (intent?.action) {
            Intent.ACTION_BATTERY_CHANGED -> {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                onBatteryChanged(level)
            }

            WifiManager.RSSI_CHANGED_ACTION, ConnectivityManager.CONNECTIVITY_ACTION -> {
                context?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        getWifiInfoApi30(it, onWifiChanged)
                    } else {
                        getWifiInfoPreApi30(it, onWifiChanged)
                    }

                    val isConnected = isOnline(it)
                    if (wasConnected == null || wasConnected != isConnected) {
                        wasConnected = isConnected
                        onConnectionChange(isConnected)

                        if (isConnected) {
                            Log.d("MyLog", "Wifi соединение было подключено")
                        } else {
                            Log.d("MyLog", "Wifi соединение было отключено")
                        }
                    }
                }
            }
        }


    }

    @Suppress("DEPRECATION")
    private fun getWifiInfoPreApi30(context: Context, onWifiChanged: (Int) -> Unit) {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val signalLevel = WifiManager.calculateSignalLevel(wifiInfo.rssi, 5)
        onWifiChanged(signalLevel)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun getWifiInfoApi30(context: Context, onWifiChanged: (Int) -> Unit) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerNetworkCallback(
            NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(),
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    val wifiInfo = networkCapabilities.transportInfo as? WifiInfo
                    wifiInfo?.let {
                        val signalLevel = WifiManager.calculateSignalLevel(it.rssi, 5)
                        onWifiChanged(signalLevel)
                    }
                }
            })
    }

    fun isOnline(context: Context?): Boolean {
        if (context == null) return false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                when {
//                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
//                        return true
//                    }

                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        return true
                    }

//                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
//                        return true
//                    }
                }
            }
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                return true
            }
        }
        return false
    }


}


