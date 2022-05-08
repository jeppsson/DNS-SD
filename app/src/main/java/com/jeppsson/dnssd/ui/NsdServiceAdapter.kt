package com.jeppsson.dnssd.ui

import android.net.nsd.NsdServiceInfo
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jeppsson.dnssd.R
import com.jeppsson.dnssd.databinding.ServiceItemBinding

/**
 * Adapter for service discovery.
 */
class NsdServiceAdapter(private val serviceClickCallback: ServiceClickCallback) :
    ListAdapter<NsdServiceInfo, NsdServiceAdapter.ServiceViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val binding = DataBindingUtil.inflate<ServiceItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.service_item, parent, false
        )
        binding.callback = serviceClickCallback
        return ServiceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        holder.binding.service = getItem(position)
    }

    private class TaskDiffCallback : DiffUtil.ItemCallback<NsdServiceInfo>() {
        override fun areItemsTheSame(
            oldItem: NsdServiceInfo,
            newItem: NsdServiceInfo
        ): Boolean {
            return oldItem.serviceName == newItem.serviceName
                    && oldItem.serviceType == newItem.serviceType
        }

        override fun areContentsTheSame(
            oldItem: NsdServiceInfo,
            newItem: NsdServiceInfo
        ): Boolean {
            return oldItem.serviceName == newItem.serviceName
                    && oldItem.serviceType == newItem.serviceType
        }
    }

    class ServiceViewHolder(val binding: ServiceItemBinding) :
        RecyclerView.ViewHolder(binding.root)
}
