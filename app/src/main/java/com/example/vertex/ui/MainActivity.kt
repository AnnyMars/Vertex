package com.example.vertex.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.vertex.R
import com.example.vertex.data.models.Configuration
import com.example.vertex.databinding.ActivityMainBinding
import com.example.vertex.utils.SystemStateReceiver

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: SharedViewModel by viewModels()

    private lateinit var batteryTextView: TextView
    private lateinit var wifiTextView: TextView

    private var isReceiverRegistered = false
    private var hasShownConfigError = false
    private var hasShownConnectionError = false

    @SuppressLint("SetTextI18n")
    private val systemStateReceiver = SystemStateReceiver(
        onBatteryChanged = { level ->
            batteryTextView.text = "Battery Level: $level%"
        },
        onWifiChanged = { signalLevel ->
            wifiTextView.text = "Wi-Fi Signal Level: $signalLevel"
        },
        onConnectionChange = { isConnected ->
            if (!isConnected) {
                showAlertDialog(
                    this,
                    getString(R.string.error_connection),
                    getString(R.string.error_connection_description)
                )
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        savedInstanceState?.let {
            hasShownConfigError = it.getBoolean("hasShownConfigError", false)
            hasShownConnectionError = it.getBoolean("hasShownConnectionError", false)
        }

        viewModel.fetchConfiguration()

        observeViewModel()

        setUpInfo()
    }

    override fun onResume() {
        super.onResume()
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(WifiManager.RSSI_CHANGED_ACTION)
                addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            }
            registerReceiver(systemStateReceiver, filter)
            isReceiverRegistered = true
        }
    }

    override fun onPause() {
        super.onPause()
        if (isReceiverRegistered) {
            unregisterReceiver(systemStateReceiver)
            isReceiverRegistered = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("hasShownConfigError", hasShownConfigError)
        outState.putBoolean("hasShownConnectionError", hasShownConnectionError)
    }

    private fun observeViewModel() {
        viewModel.configuration.observe(this) { result ->
            result.fold(
                onSuccess = { config ->
                    launchActivity(config)
                },
                onFailure = { error ->
                    if (!hasShownConfigError) {
                        if (!hasShownConnectionError) {
                            hasShownConnectionError = true
                            showAlertDialog(
                                this,
                                getString(R.string.error_connection),
                                getString(R.string.error_connection_description)
                            )
                        }
                        hasShownConfigError = true
                        showAlertDialog(
                            this,
                            getString(R.string.config_error),
                            getString(R.string.config_error_desription)
                        )
                    }
                }
            )
        }
    }

    private fun showAlertDialog(context: Context, title: String, message: String) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun launchActivity(config: Configuration) {
        config.activities.forEach { activityConfig ->
            when (activityConfig.activity) {
                "test activity" -> {
                    val intent = Intent(this, TestActivity::class.java)
                    intent.putExtra("config", activityConfig)
                    startActivity(intent)
                }

                else -> showAlertDialog(
                    this,
                    getString(R.string.request_error), getString(R.string.request_error_description)
                )
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

        val button = android.widget.Button(this).apply {
            text = "Повторить запрос"
            setOnClickListener {
                hasShownConfigError = false
                viewModel.fetchConfiguration()
            }
        }
        binding.main.addView(button)
    }
}