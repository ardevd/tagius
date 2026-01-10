package net.ardevd.tagius.features.records.ui.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.ardevd.tagius.databinding.FragmentAddRecordBinding

class AddRecordBottomSheet(
    private val initialDescription: String = "",
    private val onStartTimer: (String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: FragmentAddRecordBinding? = null
    private val binding get() = _binding!!

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

        binding.startButton.setOnClickListener {
            val description = binding.descriptionInput.text.toString().trim()

            if (description.isNotEmpty()) {
                onStartTimer(description)
                dismiss() // Close the sheet
            } else {
                binding.inputLayout.error = "Please enter a description"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddRecordBottomSheet"
    }
}