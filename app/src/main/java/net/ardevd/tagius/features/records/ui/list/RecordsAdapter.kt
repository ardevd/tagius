package net.ardevd.tagius.features.records.ui.list

import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
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
import java.util.regex.Pattern

class RecordsAdapter(
    private val onStopClick: (TimeTaggerRecord) -> Unit,
    private val onItemClick: (TimeTaggerRecord) -> Unit
) : ListAdapter<TimeTaggerRecord, RecordsAdapter.RecordViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val binding = ItemRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecordViewHolder(binding, onStopClick, onItemClick)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RecordViewHolder(
        private val binding: ItemRecordBinding,
        private val onStopClick: (TimeTaggerRecord) -> Unit,
        private val onItemClick: (TimeTaggerRecord) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        // Regex for tags (Start with #, followed by non-whitespace characters)
        private val tagPattern = Pattern.compile("#\\S+")

        private fun colorizeTags(text: String): SpannableString {
            val spannable = SpannableString(text)
            val matcher = tagPattern.matcher(text)

            // Get the color from the theme
            val color = binding.root.context.getColor(R.color.dark_orange)

            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()

                // Apply Color
                spannable.setSpan(
                    ForegroundColorSpan(color),
                    start,
                    end,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            return spannable
        }

        fun bind(record: TimeTaggerRecord) {
            val rawDescription =
                record.description.ifEmpty { binding.root.context.getString(R.string.records_no_description) }
            binding.description.text = colorizeTags(rawDescription)

            val date = record.startTime.toReadableDate()
            val start = record.startTime.toReadableTime()
            val end = record.endTime.toReadableTime()
            binding.timeInfo.text = "$date • $start - $end"

            val isRunning = record.endTime == record.startTime

            binding.root.setOnClickListener {
                onItemClick(record)
            }

            if (isRunning) {
                binding.duration.text = binding.root.context.getString(R.string.records_running)

                // Show Stop Button
                binding.stopButton.visibility = View.VISIBLE
                binding.stopButton.setOnClickListener {
                    onStopClick(record)
                }

                // Stylistic touches
                binding.cardContainer.strokeWidth = 3
                binding.cardContainer.strokeColor = binding.root.context.getColor(R.color.teal_200)

                // Apply a stroke to the card to show it's active
                binding.cardContainer.strokeWidth =
                    3
                binding.cardContainer.strokeColor =
                    binding.root.context.getColor(R.color.teal_200)


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
        override fun areItemsTheSame(oldItem: TimeTaggerRecord, newItem: TimeTaggerRecord) =
            oldItem.key == newItem.key

        override fun areContentsTheSame(oldItem: TimeTaggerRecord, newItem: TimeTaggerRecord) =
            oldItem == newItem
    }
}