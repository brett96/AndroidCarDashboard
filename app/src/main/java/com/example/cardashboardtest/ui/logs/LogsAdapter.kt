package com.example.cardashboardtest.ui.logs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cardashboardtest.R
import com.example.cardashboardtest.databinding.ItemLogBinding
import com.example.cardashboardtest.model.BluetoothLog
import com.example.cardashboardtest.model.LogType
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class LogsAdapter : ListAdapter<BluetoothLog, LogsAdapter.LogViewHolder>(LogDiffCallback()) {
    private val dateFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LogViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class LogViewHolder(
        private val binding: ItemLogBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(log: BluetoothLog) {
            binding.apply {
                deviceName.text = log.deviceName
                timestamp.text = dateFormat.format(log.timestamp)
                logType.text = log.logType.name
                message.text = log.message
                
                // Set chip background color based on log type
                val chipColor = when (log.logType) {
                    LogType.ERROR -> R.color.error_color
                    LogType.CONNECT -> R.color.success_color
                    LogType.DISCONNECT -> R.color.warning_color
                    else -> R.color.info_color
                }
                logType.setChipBackgroundColorResource(chipColor)
                
                // Show connection duration if available
                connectionDuration.visibility = if (log.connectionDuration != null) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                if (log.connectionDuration != null) {
                    connectionDuration.text = "Duration: ${log.connectionDuration / 1000}s"
                }
            }
        }
    }
    
    private class LogDiffCallback : DiffUtil.ItemCallback<BluetoothLog>() {
        override fun areItemsTheSame(oldItem: BluetoothLog, newItem: BluetoothLog): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: BluetoothLog, newItem: BluetoothLog): Boolean {
            return oldItem == newItem
        }
    }
    
    private fun formatDuration(duration: Long): String {
        val seconds = duration / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60)
            minutes > 0 -> String.format("%d:%02d", minutes, seconds % 60)
            else -> String.format("%d sec", seconds)
        }
    }
} 