package com.jeppsson.dnssd.viewmodels

import android.app.Application
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import timber.log.Timber

class NsdViewModel(application: Application) : AndroidViewModel(application),
    NsdManager.DiscoveryListener {

    private val nsdManager: NsdManager =
        (application.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager)
    private val wifiManager: WifiManager =
        (application.getSystemService(Context.WIFI_SERVICE) as WifiManager)
    private val multicastLock = wifiManager.createMulticastLock(application.packageName)

    private val serviceList: MutableList<NsdServiceInfo> = mutableListOf()
    private val _serviceInfoList: MutableLiveData<List<NsdServiceInfo>> =
        MutableLiveData(emptyList())
    private val _state: MutableLiveData<State> = MutableLiveData(Idle)

    private val handler: Handler = Handler(Looper.getMainLooper()) { message ->
        when (message.what) {
            MESSAGE_DISCOVER_TIMEOUT -> {
                stopServiceDiscovery()
                true
            }
            else -> false
        }
    }

    val serviceInfoList: LiveData<List<NsdServiceInfo>> = _serviceInfoList
    val state: LiveData<State> = _state

    init {
        _state.postValue(Idle)
    }

    override fun onCleared() {
        super.onCleared()

        stopServiceDiscovery()
    }

    override fun onDiscoveryStarted(regType: String) {
        _state.postValue(Scanning)
    }

    override fun onDiscoveryStopped(regType: String) {
        _state.postValue(Idle)
    }

    override fun onServiceFound(service: NsdServiceInfo) {
        nsdManager.resolveService(service, MyResolver(this))
    }

    override fun onServiceLost(serviceInfo: NsdServiceInfo) {
        Timber.d("Service lost: $serviceInfo")

        serviceList.remove(serviceInfo)
        _serviceInfoList.postValue(serviceList)
    }

    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
        val errorText = "Starting service discovery failed.  ${errorCodeToString(errorCode)}"
        Timber.e(errorText)
        Toast.makeText(getApplication(), errorText, Toast.LENGTH_SHORT).show()
    }

    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
        val errorText = "Stopping service discovery failed.  ${errorCodeToString(errorCode)}"
        Timber.wtf(errorText)
        Toast.makeText(getApplication(), errorText, Toast.LENGTH_SHORT).show()
    }

    fun discoverServices(serviceType: String) {
        if (handler.hasMessages(MESSAGE_DISCOVER_TIMEOUT)) {
            return
        }

        handler.sendEmptyMessageDelayed(MESSAGE_DISCOVER_TIMEOUT, DISCOVER_TIMEOUT)

        // Allows an application to receive Wifi Multicast packets. Normally the Wifi stack filters
        // out packets not explicitly addressed to this device.
        // Acquiring a MulticastLock will cause the stack to receive packets addressed to multicast
        // addresses. Processing these extra packets can cause a noticeable battery drain and
        // should be disabled when not needed.
        multicastLock?.acquire()

        serviceList.clear()
        _serviceInfoList.postValue(serviceList)

        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, this)
    }

    private fun stopServiceDiscovery() {
        try {
            nsdManager.stopServiceDiscovery(this)
        } catch (e: Exception) {
            Timber.w(e.message)
        }

        try {
            multicastLock?.release()
        } catch (e: Exception) {
            Timber.w(e.message)
        }
    }

    private class MyResolver(private val viewModel: NsdViewModel) : NsdManager.ResolveListener {

        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            val errorText = "Resolve failed: ${errorCodeToString(errorCode)}"
            Timber.e(errorText)
            Toast.makeText(viewModel.getApplication(), errorText, Toast.LENGTH_LONG).show()
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Timber.d("Resolve Succeeded. $serviceInfo")

            viewModel.serviceList.add(serviceInfo)
            viewModel._serviceInfoList.postValue(viewModel.serviceList)
        }
    }

    private companion object {

        private const val MESSAGE_DISCOVER_TIMEOUT = 1
        private const val DISCOVER_TIMEOUT: Long = 5_000

        private fun errorCodeToString(errorCode: Int): String {
            return when (errorCode) {
                NsdManager.FAILURE_ALREADY_ACTIVE -> "FAILURE_ALREADY_ACTIVE"
                NsdManager.FAILURE_INTERNAL_ERROR -> "FAILURE_INTERNAL_ERROR"
                NsdManager.FAILURE_MAX_LIMIT -> "FAILURE_MAX_LIMIT"
                else -> "unknown"
            }
        }
    }

    sealed class State
    object Idle : State()
    object Scanning : State()
}
