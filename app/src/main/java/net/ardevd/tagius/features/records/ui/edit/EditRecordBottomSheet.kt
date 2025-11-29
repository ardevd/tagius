package net.ardevd.tagius.features.records.ui.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.ardevd.tagius.core.data.TimeTaggerRecord
import net.ardevd.tagius.databinding.FragmentEditRecordBinding

class EditRecordBottomSheet(
    private val record: TimeTaggerRecord,
    private val onSave: (String) -> Unit,
    private val onDelete: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: FragmentEditRecordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pre-fill data
        binding.descriptionInput.setText(record.description)

        // Save Logic
        binding.saveButton.setOnClickListener {
            val newDesc = binding.descriptionInput.text.toString().trim()
            if (newDesc.isNotEmpty()) {
                binding.inputLayout.error = null
                onSave(newDesc)
                dismiss()
            } else {
                binding.inputLayout.error = "Please enter a description"
            }
        }

        // Delete Logic with Confirmation
        binding.deleteButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Record?")
                .setMessage("Are you sure you want to remove this time entry?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ ->
                    onDelete()
                    dismiss()
                }
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "EditRecordBottomSheet"
    }
}