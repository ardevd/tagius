package net.ardevd.tagius.features.records.ui.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import net.ardevd.tagius.core.data.TimeTaggerRecord
import net.ardevd.tagius.databinding.FragmentEditRecordBinding
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditRecordBottomSheet(
    private val record: TimeTaggerRecord,
    // Callback now takes Description, Start(Long), End(Long)
    private val onSave: (String, Long, Long) -> Unit, private val onDelete: () -> Unit
) : BottomSheetDialogFragment() {
    private var _binding: FragmentEditRecordBinding? = null
    private val binding get() = _binding!!

    // State to hold the edits before saving
    private var currentStart: Long = record.startTime
    private var currentEnd: Long = record.endTime
    private val displayFormatter = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun updateTimeDisplays() {
        binding.startInput.setText(displayFormatter.format(Date(currentStart * 1000)))
        binding.endInput.setText(displayFormatter.format(Date(currentEnd * 1000)))

        // Clear error if valid
        if (currentEnd >= currentStart) binding.endInputLayout.error = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pre-fill data
        binding.descriptionInput.setText(record.description)

        updateTimeDisplays()

        // Setup Time Inputs
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

        // Save Logic
        binding.saveButton.setOnClickListener {
            // Basic validation: End cannot be before Start
            if (currentEnd < currentStart) {
                binding.endInputLayout.error = "End time cannot be before start"
            } else {
                val newDesc = binding.descriptionInput.text.toString().trim()
                if (newDesc.isNotEmpty()) {
                    binding.inputLayout.error = null
                    onSave(newDesc, currentStart, currentEnd)
                    dismiss()
                } else {
                    binding.inputLayout.error = "Please enter a description"
                }
            }
        }

        // Delete Logic with Confirmation
        binding.deleteButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext()).setTitle("Delete Record?")
                .setMessage("Are you sure you want to remove this time entry?")
                .setNegativeButton("Cancel", null).setPositiveButton("Delete") { _, _ ->
                    onDelete()
                    dismiss()
                }.show()
        }
    }

    /**
     * Shows Date Picker -> Then Time Picker -> Returns Unix Seconds
     */
    private fun showDateTimePicker(initialTimestamp: Long, onTimeSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = initialTimestamp * 1000

        // 1. Date Picker
        val datePicker = MaterialDatePicker.Builder.datePicker().setTitleText("Select date")
            .setSelection(calendar.timeInMillis).build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            // Update Calendar with selected Date (selection is UTC)
            val selectedDate = Calendar.getInstance()
            selectedDate.timeInMillis = selection

            // 2. Time Picker
            val isSystem24Hour = DateFormat.is24HourFormat(requireContext())
            val clockFormat = if (isSystem24Hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H

            val timePicker = MaterialTimePicker.Builder().setTimeFormat(clockFormat)
                .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(calendar.get(Calendar.MINUTE)).setTitleText("Select time").build()

            timePicker.addOnPositiveButtonClickListener {
                // Combine Date + Time
                // Create a new calendar in local timezone
                val finalCalendar = Calendar.getInstance()
                finalCalendar.set(
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH),
                    timePicker.hour,
                    timePicker.minute
                )

                // Return Seconds
                onTimeSelected(finalCalendar.timeInMillis / 1000)
            }

            timePicker.show(parentFragmentManager, "timePicker")
        }

        datePicker.show(parentFragmentManager, "datePicker")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "EditRecordBottomSheet"
    }
}