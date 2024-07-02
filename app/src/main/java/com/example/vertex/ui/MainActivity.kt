package com.example.vertex.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.vertex.data.models.Configuration
import com.example.vertex.databinding.ActivityMainBinding
import com.example.vertex.utils.SystemStateReceiver

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: SharedViewModel by viewModels()

    private lateinit var batteryTextView: TextView
    private lateinit var wifiTextView: TextView

    @SuppressLint("SetTextI18n")
    private val systemStateReceiver = SystemStateReceiver(
        onBatteryChanged = { level ->
            batteryTextView.text = "Battery Level: $level%"
        },
        onWifiChanged = { signalLevel ->
            wifiTextView.text = "Wi-Fi Signal Level: $signalLevel"
        },
        onConnectionChange = { isConnected ->
            if (!isConnected) showError("Wifi отключен")
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.fetchConfiguration()

        viewModel.configuration.observe(this) { result ->

            result.fold(
                onSuccess = { config ->
                    launchActivity(config)
                },
                onFailure = { error ->
                    showError("Ошибка загрузки данных- ${error.message}")
                }
            )
        }
        setUpInfo()


        registerReceiver()
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(systemStateReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(systemStateReceiver)
    }

    private fun launchActivity(config: Configuration) {
        config.activities.forEach { activityConfig ->
            when (activityConfig.activity) {
                "test activity" -> {
                    val intent = Intent(this, TestActivity::class.java)
                    intent.putExtra("config", activityConfig)
                    startActivity(intent)
                }

                else -> showError("Неизвестный тип activity: ${activityConfig.activity}")
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setUpInfo() {
        batteryTextView = TextView(this).apply {
            textSize = 16f
            setPadding(0, 10, 0, 10)
            text = "Battery Level: N/A"
        }
        binding.main.addView(batteryTextView)

        wifiTextView = TextView(this).apply {
            textSize = 16f
            setPadding(0, 10, 0, 10)
            text = "Wi-Fi Signal Level: N/A"
        }
        binding.main.addView(wifiTextView)
    }

    private fun registerReceiver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(systemStateReceiver, IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(WifiManager.RSSI_CHANGED_ACTION)
                addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            }, RECEIVER_NOT_EXPORTED)
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}