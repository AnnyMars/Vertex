package com.example.vertex.ui

import android.R
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.vertex.data.models.ActivityConfig
import com.example.vertex.data.models.User
import com.example.vertex.databinding.ActivityTestBinding
import com.example.vertex.utils.Constants.BASE_URL
import com.example.vertex.utils.SystemStateReceiver

class TestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTestBinding
    private val textFieldsMap = mutableMapOf<String, AutoCompleteTextView>()

    private val viewModel: SharedViewModel by viewModels()
    private lateinit var fullNameEditText: EditText
    private lateinit var positionAutoCompleteTextView: AutoCompleteTextView
    private lateinit var workHoursTextView: TextView
    private lateinit var workedOutHoursTextView: TextView
    private lateinit var batteryTextView: TextView
    private lateinit var wifiTextView: TextView

    private var isReceiverRegistered = false

    private var hasShownRequestError = false

    @SuppressLint("SetTextI18n")
    private val systemStateReceiver = SystemStateReceiver(
        onBatteryChanged = { level ->
            batteryTextView.text = "Battery Level: $level%"
        },
        onWifiChanged = { signalLevel ->
            wifiTextView.text = "Wi-Fi Signal Level: $signalLevel"
        },
        onConnectionChange = { isConnected ->
            if (!isConnected) showAlertDialog(
                this,
                getString(com.example.vertex.R.string.error_connection),
                getString(com.example.vertex.R.string.error_connection_description)
            )
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        savedInstanceState?.let {
            hasShownRequestError = it.getBoolean("hasShownRequestError", false)
        }

        val config = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("config", ActivityConfig::class.java)
        } else {
            intent.getSerializableExtra("config") as? ActivityConfig
        }
        if (config != null) {
            setupUI(config)
        } else {
            showAlertDialog(
                this,
                getString(com.example.vertex.R.string.config_error),
                getString(com.example.vertex.R.string.config_error_desription)
            )
        }


        observeViewModel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("hasShownRequestError", hasShownRequestError)
    }

    override fun onStart() {
        super.onStart()
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(ConnectivityManager.CONNECTIVITY_ACTION)
                addAction(WifiManager.RSSI_CHANGED_ACTION)
            }
            registerReceiver(systemStateReceiver, filter)
            isReceiverRegistered = true
        }
    }


    override fun onStop() {
        super.onStop()
        if (isReceiverRegistered) {
            unregisterReceiver(systemStateReceiver)
            isReceiverRegistered = false
        }

    }

    @SuppressLint("SetTextI18n")
    private fun setupUI(config: ActivityConfig) {
        val rootLayout = binding.rootLayout
        val headerTextView = TextView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            text = config.layout.header
            textSize = 24f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 20, 0, 20)
        }
        rootLayout.addView(headerTextView)

        config.layout.form.text?.forEach { textField ->
            when (textField.type) {
                "plain-text" -> {
                    val editText = EditText(this).apply {
                        hint = textField.caption
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                    if (textField.attribute == "full-name") {
                        fullNameEditText = editText
                    }
                    rootLayout.addView(editText)
                }

                "auto-complete-text-view" -> {
                    val autoCompleteTextView = AutoCompleteTextView(this).apply {
                        hint = textField.caption
                        textField.suggestions?.let {
                            setAdapter(
                                ArrayAdapter(
                                    this@TestActivity,
                                    R.layout.simple_dropdown_item_1line,
                                    it
                                )
                            )
                        }
                    }
                    if (textField.attribute == "position") {
                        positionAutoCompleteTextView = autoCompleteTextView
                    }
                    textFieldsMap[textField.attribute] = autoCompleteTextView
                    rootLayout.addView(autoCompleteTextView)
                }
            }
        }

        workHoursTextView = TextView(this).apply {
            textSize = 16f
            setPadding(0, 10, 0, 10)
        }
        rootLayout.addView(workHoursTextView)

        workedOutHoursTextView = TextView(this).apply {
            textSize = 16f
            setPadding(0, 10, 0, 10)
        }
        rootLayout.addView(workedOutHoursTextView)

        batteryTextView = TextView(this).apply {
            textSize = 16f
            setPadding(0, 10, 0, 10)
            text = "Battery Level: N/A"
        }
        rootLayout.addView(batteryTextView)

        wifiTextView = TextView(this).apply {
            textSize = 16f
            setPadding(0, 10, 0, 10)
            text = "Wi-Fi Signal Level: N/A"
        }
        rootLayout.addView(wifiTextView)

        config.layout.form.buttons?.forEach { buttonConfig ->
            val button = Button(this).apply {
                text = buttonConfig.caption
                setOnClickListener {
                    handleUserResponse(buttonConfig)
                }
            }
            rootLayout.addView(button)
        }
    }

    private fun handleUserResponse(button: com.example.vertex.data.models.Button) {
        if (hasShownRequestError) {
            showAlertDialog(
                this,
                getString(com.example.vertex.R.string.data_error),
                getString(com.example.vertex.R.string.data_error_description)
            )
            hasShownRequestError = !hasShownRequestError
            return
        }
        when (button.formAction) {
            "/" -> viewModel.fetchUserData("")
            else -> {
                viewModel.fetchUserData(BASE_URL + button.formAction)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.data.observe(this) { result ->
            result.fold(
                onSuccess = { userResponse ->
                    if (userResponse.error.isError) {
                        showAlertDialog(
                            this,
                            getString(com.example.vertex.R.string.request_error),
                            userResponse.error.description
                        )
                    } else {
                        updateUIWithUserData(userResponse.data.user)
                    }
                },
                onFailure = {
                    if (!hasShownRequestError) {
                        hasShownRequestError = true
                        showAlertDialog(
                            this,
                            getString(com.example.vertex.R.string.data_error),
                            getString(com.example.vertex.R.string.data_error_description)
                        )
                    }

                }
            )
        }
    }

    @SuppressLint("SetTextI18n", "StringFormatMatches")
    private fun updateUIWithUserData(user: User) {
        fullNameEditText.setText(user.fullName)
        positionAutoCompleteTextView.setText(user.position)
        workHoursTextView.text =
            getString(com.example.vertex.R.string.work_hours_text, user.workHoursInMonth)
        workedOutHoursTextView.text =
            getString(com.example.vertex.R.string.worked_out_hours_text, user.workedOutHours)
    }

    private fun showAlertDialog(context: Context, title: String, message: String) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

}