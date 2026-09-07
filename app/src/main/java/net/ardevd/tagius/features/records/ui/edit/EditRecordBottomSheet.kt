package net.ardevd.tagius.features.records.ui.edit

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import net.ardevd.tagius.core.data.TimeTaggerRecord
import net.ardevd.tagius.databinding.FragmentEditRecordBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditRecordBottomSheet : BottomSheetDialogFragment() {
    private var _binding: FragmentEditRecordBinding? = null
    private val binding get() = _binding!!
    private val record: TimeTaggerRecord by lazy { recordFromBundle(requireArguments()) }
    private var currentStart = 0L
    private var currentEnd = 0L
    private val displayFormatter = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentStart = savedInstanceState?.getLong(STATE_CURRENT_START) ?: record.startTime
        currentEnd = savedInstanceState?.getLong(STATE_CURRENT_END) ?: record.endTime
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentEditRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            binding.descriptionInput.setText(record.description)
        }
        updateTimeDisplays()

        binding.startInput.setOnClickListener {
            showDateTimePicker(currentStart) { newTime ->
                currentStart = newTime
                updateTimeDisplays()
            }
        }

        binding.endInput.setOnClickListener {
            showDateTimePicker(currentEnd) { newTime ->
                currentEnd = newTime
                updateTimeDisplays()
            }
        }

        binding.saveButton.setOnClickListener {
            if (currentEnd < currentStart) {
                binding.endInputLayout.error = "End time cannot be before start"
            } else {
                val newDescription =
                    binding.descriptionInput.text
                        .toString()
                        .trim()
                if (newDescription.isNotEmpty()) {
                    binding.inputLayout.error = null
                    setFragmentResult(
                        REQUEST_KEY,
                        resultBundle(ACTION_SAVE).apply {
                            putString(RESULT_DESCRIPTION, newDescription)
                            putLong(RESULT_START_TIME, currentStart)
                            putLong(RESULT_END_TIME, currentEnd)
                        },
                    )
                    dismiss()
                } else {
                    binding.inputLayout.error = "Please enter a description"
                }
            }
        }

        binding.deleteButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Record?")
                .setMessage("Are you sure you want to remove this time entry?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ ->
                    setFragmentResult(REQUEST_KEY, resultBundle(ACTION_DELETE))
                    dismiss()
                }.show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(STATE_CURRENT_START, currentStart)
        outState.putLong(STATE_CURRENT_END, currentEnd)
        super.onSaveInstanceState(outState)
    }

    private fun resultBundle(action: String) =
        recordToBundle(record).apply {
            putString(RESULT_ACTION, action)
        }

    private fun updateTimeDisplays() {
        binding.startInput.setText(displayFormatter.format(Date(currentStart * 1000)))
        binding.endInput.setText(displayFormatter.format(Date(currentEnd * 1000)))
        if (currentEnd >= currentStart) binding.endInputLayout.error = null
    }

    private fun showDateTimePicker(
        initialTimestamp: Long,
        onTimeSelected: (Long) -> Unit,
    ) {
        val calendar =
            Calendar.getInstance().apply {
                timeInMillis = initialTimestamp * 1000
            }

        val datePicker =
            MaterialDatePicker.Builder
                .datePicker()
                .setTitleText("Select date")
                .setSelection(calendar.timeInMillis)
                .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val selectedDate =
                Calendar.getInstance().apply {
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
                    .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                    .setMinute(calendar.get(Calendar.MINUTE))
                    .setTitleText("Select time")
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
                        )
                    }
                onTimeSelected(finalCalendar.timeInMillis / 1000)
            }

            timePicker.show(parentFragmentManager, TIME_PICKER_TAG)
        }

        datePicker.show(parentFragmentManager, DATE_PICKER_TAG)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "EditRecordBottomSheet"
        const val REQUEST_KEY = "edit_record_result"
        const val RESULT_ACTION = "action"
        const val RESULT_DESCRIPTION = "description"
        const val RESULT_START_TIME = "new_start_time"
        const val RESULT_END_TIME = "new_end_time"
        const val ACTION_SAVE = "save"
        const val ACTION_DELETE = "delete"

        private const val ARG_RECORD_KEY = "record_key"
        private const val ARG_RECORD_START = "record_start"
        private const val ARG_RECORD_END = "record_end"
        private const val ARG_RECORD_MODIFIED = "record_modified"
        private const val ARG_RECORD_DESCRIPTION = "record_description"
        private const val ARG_RECORD_SERVER_TIME = "record_server_time"
        private const val STATE_CURRENT_START = "current_start"
        private const val STATE_CURRENT_END = "current_end"
        private const val DATE_PICKER_TAG = "datePicker"
        private const val TIME_PICKER_TAG = "timePicker"

        fun newInstance(record: TimeTaggerRecord) =
            EditRecordBottomSheet().apply {
                arguments = recordToBundle(record)
            }

        fun recordFromResult(result: Bundle): TimeTaggerRecord = recordFromBundle(result)

        private fun recordToBundle(record: TimeTaggerRecord) =
            Bundle().apply {
                putString(ARG_RECORD_KEY, record.key)
                putLong(ARG_RECORD_START, record.startTime)
                putLong(ARG_RECORD_END, record.endTime)
                putLong(ARG_RECORD_MODIFIED, record.modifiedTime)
                putString(ARG_RECORD_DESCRIPTION, record.description)
                putDouble(ARG_RECORD_SERVER_TIME, record.serverTime)
            }

        private fun recordFromBundle(bundle: Bundle) =
            TimeTaggerRecord(
                key = requireNotNull(bundle.getString(ARG_RECORD_KEY)),
                startTime = bundle.getLong(ARG_RECORD_START),
                endTime = bundle.getLong(ARG_RECORD_END),
                modifiedTime = bundle.getLong(ARG_RECORD_MODIFIED),
                description = requireNotNull(bundle.getString(ARG_RECORD_DESCRIPTION)),
                serverTime = bundle.getDouble(ARG_RECORD_SERVER_TIME),
            )
    }
}
