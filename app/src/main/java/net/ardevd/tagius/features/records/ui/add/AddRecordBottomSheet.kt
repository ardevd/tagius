package net.ardevd.tagius.features.records.ui.add

import android.os.Bundle
import android.text.format.DateFormat
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
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

class AddRecordBottomSheet : BottomSheetDialogFragment() {
    private var _binding: FragmentAddRecordBinding? = null
    private val binding get() = _binding!!
    private var selectedStartTime = 0L
    private val initialDescription: String
        get() = requireArguments().getString(ARG_INITIAL_DESCRIPTION).orEmpty()
    private val suggestedTags: List<String>
        get() = requireArguments().getStringArrayList(ARG_SUGGESTED_TAGS).orEmpty()
    private val displayFormatter by lazy {
        val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMM d yyyy jm")
        SimpleDateFormat(pattern, Locale.getDefault())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedStartTime = savedInstanceState?.getLong(STATE_SELECTED_START_TIME)
            ?: (System.currentTimeMillis() / 1000 - DEFAULT_EARLIER_OFFSET_SECONDS)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAddRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null && initialDescription.isNotEmpty()) {
            binding.descriptionInput.setText(initialDescription)
            binding.descriptionInput.selectAll()
        }

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
            val description =
                binding.descriptionInput.text
                    .toString()
                    .trim()
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
                    setFragmentResult(
                        REQUEST_KEY,
                        Bundle().apply {
                            putString(RESULT_DESCRIPTION, description)
                            putBoolean(RESULT_HAS_START_TIME, startsEarlier)
                            putLong(RESULT_START_TIME, selectedStartTime)
                        },
                    )
                    it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    dismiss()
                }
            }
        }

        setupTagSuggestions()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(STATE_SELECTED_START_TIME, selectedStartTime)
        super.onSaveInstanceState(outState)
    }

    private fun setupTagSuggestions() {
        if (suggestedTags.isEmpty()) {
            binding.tagScroll.visibility = View.GONE
            return
        }

        binding.tagScroll.visibility = View.VISIBLE
        binding.tagChipGroup.removeAllViews()

        suggestedTags.forEach { tag ->
            val chip =
                layoutInflater.inflate(
                    R.layout.item_suggestion_chip,
                    binding.tagChipGroup,
                    false,
                ) as Chip
            chip.text = tag
            chip.setOnClickListener { appendTag(tag) }
            binding.tagChipGroup.addView(chip)
        }
    }

    private fun updateStartTimeDisplay() {
        binding.startTimeInput.setText(displayFormatter.format(Date(selectedStartTime * 1000)))
    }

    private fun showDateTimePicker(
        initialTimestamp: Long,
        onTimeSelected: (Long) -> Unit,
    ) {
        val initialCalendar =
            Calendar.getInstance().apply {
                timeInMillis = initialTimestamp * 1000
            }
        val initialDateSelection =
            Calendar
                .getInstance(TimeZone.getTimeZone("UTC"))
                .apply {
                    clear()
                    set(
                        initialCalendar.get(Calendar.YEAR),
                        initialCalendar.get(Calendar.MONTH),
                        initialCalendar.get(Calendar.DAY_OF_MONTH),
                    )
                }.timeInMillis
        val datePicker =
            MaterialDatePicker.Builder
                .datePicker()
                .setTitleText(R.string.timer_select_start_date)
                .setSelection(initialDateSelection)
                .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val selectedDate =
                Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    timeInMillis = selection
                }
            val clockFormat =
                if (DateFormat.is24HourFormat(requireContext())) {
                    TimeFormat.CLOCK_24H
                } else {
                    TimeFormat.CLOCK_12H
                }
            val timePicker =
                MaterialTimePicker
                    .Builder()
                    .setTimeFormat(clockFormat)
                    .setHour(initialCalendar.get(Calendar.HOUR_OF_DAY))
                    .setMinute(initialCalendar.get(Calendar.MINUTE))
                    .setTitleText(R.string.timer_select_start_time)
                    .build()

            timePicker.addOnPositiveButtonClickListener {
                val finalCalendar =
                    Calendar.getInstance().apply {
                        set(
                            selectedDate.get(Calendar.YEAR),
                            selectedDate.get(Calendar.MONTH),
                            selectedDate.get(Calendar.DAY_OF_MONTH),
                            timePicker.hour,
                            timePicker.minute,
                            0,
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
        val escapedTag = Regex.escape(tag)
        val tagPattern = Regex("(?<!\\S)$escapedTag(?!\\S)")
        if (tagPattern.containsMatchIn(currentText)) return

        val prefix = if (currentText.isNotEmpty() && !currentText.endsWith(" ")) " " else ""
        val newText = "$currentText$prefix$tag "
        binding.descriptionInput.setText(newText)
        binding.descriptionInput.setSelection(newText.length)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddRecordBottomSheet"
        const val REQUEST_KEY = "add_record_result"
        const val RESULT_DESCRIPTION = "description"
        const val RESULT_HAS_START_TIME = "has_start_time"
        const val RESULT_START_TIME = "start_time"

        private const val ARG_INITIAL_DESCRIPTION = "initial_description"
        private const val ARG_SUGGESTED_TAGS = "suggested_tags"
        private const val STATE_SELECTED_START_TIME = "selected_start_time"
        private const val DEFAULT_EARLIER_OFFSET_SECONDS = 15 * 60
        private const val DATE_PICKER_TAG = "startDatePicker"
        private const val TIME_PICKER_TAG = "startTimePicker"

        fun newInstance(
            initialDescription: String = "",
            suggestedTags: List<String> = emptyList(),
        ) = AddRecordBottomSheet().apply {
            arguments =
                Bundle().apply {
                    putString(ARG_INITIAL_DESCRIPTION, initialDescription)
                    putStringArrayList(ARG_SUGGESTED_TAGS, ArrayList(suggestedTags))
                }
        }
    }
}
