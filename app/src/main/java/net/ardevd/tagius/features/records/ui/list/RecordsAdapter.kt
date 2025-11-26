package net.ardevd.tagius.features.records.ui.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.ardevd.tagius.R
import net.ardevd.tagius.core.data.TimeTaggerRecord
import net.ardevd.tagius.core.utils.getDurationString
import net.ardevd.tagius.core.utils.toReadableDate
import net.ardevd.tagius.core.utils.toReadableTime
import net.ardevd.tagius.databinding.ItemRecordBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordsAdapter(
    private val onStopClick: (TimeTaggerRecord) -> Unit
) : ListAdapter<TimeTaggerRecord, RecordsAdapter.RecordViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val binding = ItemRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecordViewHolder(binding, onStopClick)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RecordViewHolder(
        private val binding: ItemRecordBinding,
        private val onStopClick: (TimeTaggerRecord) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
    fun bind(record: TimeTaggerRecord) {
            binding.description.text = record.description.ifEmpty { "No Description" }

            val date = record.startTime.toReadableDate()
            val start = record.startTime.toReadableTime()
            val end = record.endTime.toReadableTime()
            binding.timeInfo.text = "$date • $start - $end"

            val isRunning = record.endTime == record.startTime

            if (isRunning) {
                binding.duration.text = "Running"

                // Show Stop Button
                binding.stopButton.visibility = View.VISIBLE
                binding.stopButton.setOnClickListener {
                    onStopClick(record)
                }

                // Stylistic touches (optional border)
                binding.cardContainer.strokeWidth = 3
                binding.cardContainer.strokeColor = binding.root.context.getColor(R.color.teal_200)

                // Apply a stroke to the card to show it's active
                binding.cardContainer.strokeWidth = 3 // dp equivalent (needs conversion in real app)
                binding.cardContainer.strokeColor = binding.root.context.getColor(R.color.teal_200) // Or use ?attr/colorPrimary

                // Optional: Subtle background tint
                // binding.cardContainer.setCardBackgroundColor(...)
            } else {
                // STOPPED STATE:
                // Calculate and show duration
                binding.duration.text = getDurationString(record.startTime, record.endTime)

                // Hide Stop Button
                binding.stopButton.visibility = View.INVISIBLE
                binding.stopButton.setOnClickListener(null)

                // Reset style
                binding.cardContainer.strokeWidth = 0
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<TimeTaggerRecord>() {
        override fun areItemsTheSame(oldItem: TimeTaggerRecord, newItem: TimeTaggerRecord) = oldItem.key == newItem.key
        override fun areContentsTheSame(oldItem: TimeTaggerRecord, newItem: TimeTaggerRecord) = oldItem == newItem
    }
}