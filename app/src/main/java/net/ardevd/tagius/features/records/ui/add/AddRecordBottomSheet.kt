package net.ardevd.tagius.features.records.ui.add

import android.os.Bundle
import android.text.format.DateFormat
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import net.ardevd.tagius.R
import net.ardevd.tagius.databinding.FragmentAddRecordBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AddRecordBottomSheet(
    private val initialDescription: String = "",
    private val suggestedTags: List<String> = emptyList(),
    private val onStartTimer: (String, Long?) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: FragmentAddRecordBinding? = null
    private val binding get() = _binding!!
    private var selectedStartTime = System.currentTimeMillis() / 1000 - DEFAULT_EARLIER_OFFSET_SECONDS
    private val displayFormatter by lazy {
        val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMM d yyyy jm")
        SimpleDateFormat(pattern, Locale.getDefault())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pre-fill the text
        if (initialDescription.isNotEmpty()) {
            binding.descriptionInput.setText(initialDescription)
            // Optional: Select all text so the user can easily overwrite it if they want
            binding.descriptionInput.selectAll()
        }

        // Focus the input immediately (Optional UX improvement)
        binding.descriptionInput.requestFocus()

        updateStartTimeDisplay()
        binding.startTimeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            val startsEarlier = checkedId == R.id.startEarlierButton
            binding.startTimeInputLayout.isVisible = startsEarlier
            binding.startTimeInputLayout.error = null
        }
        binding.startTimeInput.setOnClickListener {
            showDateTimePicker(selectedStartTime) { startTime ->
                selectedStartTime = startTime
                updateStartTimeDisplay()
            }
        }

        binding.startButton.setOnClickListener {
            val description = binding.descriptionInput.text.toString().trim()
            val startsEarlier = binding.startEarlierButton.isChecked
            val now = System.currentTimeMillis() / 1000
            when {
                description.isEmpty() -> {
                    binding.inputLayout.error = getString(R.string.timer_description_required)
                    binding.startTimeInputLayout.error = null
                }
                startsEarlier && selectedStartTime > now -> {
                    binding.inputLayout.error = null
                    binding.startTimeInputLayout.error = getString(R.string.error_start_time_future)
                }
                else -> {
                    binding.inputLayout.error = null
                    binding.startTimeInputLayout.error = null
                    onStartTimer(description, selectedStartTime.takeIf { startsEarlier })
                    it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    dismiss()
                }
            }
        }

        setupTagSuggestions()
    }

    private fun setupTagSuggestions() {
        if (suggestedTags.isEmpty()) {
            binding.tagScroll.visibility = View.GONE
            return
        }

        binding.tagScroll.visibility = View.VISIBLE
        binding.tagChipGroup.removeAllViews()

        suggestedTags.forEach { tag ->
            val chip = layoutInflater.inflate(
                R.layout.item_suggestion_chip,
                binding.tagChipGroup,
                false
            ) as Chip
            chip.text = tag

            // Logic: Append tag to text
            chip.setOnClickListener {
                appendTag(tag)
            }

            binding.tagChipGroup.addView(chip)
        }
    }

    private fun updateStartTimeDisplay() {
        binding.startTimeInput.setText(displayFormatter.format(Date(selectedStartTime * 1000)))
    }

    private fun showDateTimePicker(initialTimestamp: Long, onTimeSelected: (Long) -> Unit) {
        val initialCalendar = Calendar.getInstance().apply {
            timeInMillis = initialTimestamp * 1000
        }
        val initialDateSelection = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                initialCalendar.get(Calendar.YEAR),
                initialCalendar.get(Calendar.MONTH),
                initialCalendar.get(Calendar.DAY_OF_MONTH)
            )
        }.timeInMillis
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.timer_select_start_date)
            .setSelection(initialDateSelection)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val selectedDate = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = selection
            }
            val clockFormat = if (DateFormat.is24HourFormat(requireContext())) {
                TimeFormat.CLOCK_24H
            } else {
                TimeFormat.CLOCK_12H
            }
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(clockFormat)
                .setHour(initialCalendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(initialCalendar.get(Calendar.MINUTE))
                .setTitleText(R.string.timer_select_start_time)
                .build()

            timePicker.addOnPositiveButtonClickListener {
                val finalCalendar = Calendar.getInstance().apply {
                    set(
                        selectedDate.get(Calendar.YEAR),
                        selectedDate.get(Calendar.MONTH),
                        selectedDate.get(Calendar.DAY_OF_MONTH),
                        timePicker.hour,
                        timePicker.minute,
                        0
                    )
                    set(Calendar.MILLISECOND, 0)
                }
                onTimeSelected(finalCalendar.timeInMillis / 1000)
            }
            timePicker.show(parentFragmentManager, TIME_PICKER_TAG)
        }
        datePicker.show(parentFragmentManager, DATE_PICKER_TAG)
    }

    private fun appendTag(tag: String) {
        val currentText = binding.descriptionInput.text.toString()

        // Don't add if already present as a separate tag/word
        val escapedTag = Regex.escape(tag)
        val tagPattern = Regex("(?<!\\S)$escapedTag(?!\\S)")
        if (tagPattern.containsMatchIn(currentText)) return

        // Add space if needed
        val prefix = if (currentText.isNotEmpty() && !currentText.endsWith(" ")) " " else ""

        val newText = "$currentText$prefix$tag " // Add trailing space for next word
        binding.descriptionInput.setText(newText)

        // Move cursor to end
        binding.descriptionInput.setSelection(newText.length)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddRecordBottomSheet"
        private const val DEFAULT_EARLIER_OFFSET_SECONDS = 15 * 60
        private const val DATE_PICKER_TAG = "startDatePicker"
        private const val TIME_PICKER_TAG = "startTimePicker"
    }
}