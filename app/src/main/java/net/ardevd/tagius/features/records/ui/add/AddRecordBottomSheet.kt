package net.ardevd.tagius.features.records.ui.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import net.ardevd.tagius.R
import net.ardevd.tagius.databinding.FragmentAddRecordBinding

class AddRecordBottomSheet(
    private val initialDescription: String = "",
    private val suggestedTags: List<String> = emptyList(),
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

    private fun appendTag(tag: String) {
        val currentText = binding.descriptionInput.text.toString()

        // Don't add if already present
        if (currentText.contains(tag)) return

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
    }
}