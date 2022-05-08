package com.jeppsson.dnssd.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.nsd.NsdManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jeppsson.dnssd.R
import com.jeppsson.dnssd.viewmodels.NsdViewModel
import timber.log.Timber

class MainActivity : AppCompatActivity(), View.OnClickListener, RadioGroup.OnCheckedChangeListener {

    private val model: NsdViewModel by viewModels()
    private val receiver = NsdStateReceiver()
    private val serviceAdapter = NsdServiceAdapter { service ->
        MaterialAlertDialogBuilder(this)
            .setMessage("$service.attributes")
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private lateinit var txtState: TextView
    private lateinit var editServiceType: EditText
    private lateinit var btnSearch: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        configureServiceList()

        txtState = findViewById(R.id.txt_state)
        editServiceType = findViewById<EditText?>(R.id.edit_service_type)
            .apply { addTextChangedListener { doAfterTextChanged { updateServiceType() } } }
        btnSearch =
            findViewById<Button>(R.id.btn_search).apply { setOnClickListener(this@MainActivity) }
        findViewById<RadioGroup>(R.id.radio_protocol).setOnCheckedChangeListener(this)

        model.serviceInfoList.observe(this) { serviceInfoList ->
            serviceAdapter.submitList(serviceInfoList.toList())
        }

        model.state.observe(this) { state ->
            btnSearch.isEnabled = state == NsdViewModel.Idle
        }

        updateServiceType()
    }

    override fun onStart() {
        super.onStart()

        registerReceiver(receiver, IntentFilter(NsdManager.ACTION_NSD_STATE_CHANGED))
    }

    override fun onStop() {
        super.onStop()

        unregisterReceiver(receiver)
    }

    override fun onClick(view: View) {
        runCatching {
            model.discoverServices(findViewById<TextView>(R.id.txt_service_type).text.toString())
        }.onFailure { exception ->
            Toast.makeText(this, exception.message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCheckedChanged(radioGroup: RadioGroup, checkedId: Int) {
        updateServiceType()
    }

    private fun updateServiceType() {
        val serviceName = editServiceType.editableText.toString().trim()

        val checkedId = findViewById<RadioGroup>(R.id.radio_protocol).checkedRadioButtonId
        val serviceProtocol =
            if (checkedId == R.id.radio_tcp) {
                "tcp"
            } else {
                "udp"
            }

        val serviceTypeText = "_$serviceName._$serviceProtocol."

        findViewById<TextView>(R.id.txt_service_type).text = serviceTypeText
    }

    /**
     * Configures the service list view.
     */
    private fun configureServiceList() {
        with(findViewById<RecyclerView>(R.id.service_list)) {
            // For performance
            setHasFixedSize(true)

            // Attach the device adapter.
            adapter = serviceAdapter
        }
    }

    private inner class NsdStateReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(NsdManager.EXTRA_NSD_STATE, -1)

            Timber.d("state: ${stateToString(state)}")
            txtState.text = stateToString(state)
        }
    }

    private companion object {

        private fun stateToString(state: Int): String {
            return when (state) {
                NsdManager.NSD_STATE_ENABLED -> "NSD_STATE_ENABLED"
                NsdManager.NSD_STATE_DISABLED -> "NSD_STATE_DISABLED"
                else -> "unknown"
            }
        }
    }
}
