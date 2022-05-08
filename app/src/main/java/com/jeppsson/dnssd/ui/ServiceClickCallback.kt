package com.jeppsson.dnssd.ui

import android.net.nsd.NsdServiceInfo

/**
 * Callback interface for handling clicks on Service.
 *
 * Called from layout with data binding.
 */
fun interface ServiceClickCallback {

    /**
     * 'Discover' has been clicked.
     */
    fun onAttributesClick(device: NsdServiceInfo)
}
