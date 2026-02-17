package net.ardevd.tagius.features.settings.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.ardevd.tagius.BuildConfig
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ardevd.tagius.core.data.TokenManager
import net.ardevd.tagius.databinding.FragmentSettingsBinding

class SettingsBottomSheet(
    private val onLogout: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tokenManager = TokenManager(requireContext())
        // Show version info
        val version = BuildConfig.VERSION_NAME
        binding.versionText.text = "v$version"

        // Display the stored URL so the user knows which server they are on
        viewLifecycleOwner.lifecycleScope.launch {
            val url = tokenManager.serverUrlFlow.first()
            binding.serverUrlText.text = url
        }

        // Handle Logout
        binding.logoutButton.setOnClickListener {
            dismiss()
            onLogout()
        }

        // System Color setting handling
        viewLifecycleOwner.lifecycleScope.launch {
            val useDynamicColors = tokenManager.dynamicColorsFlow.first()
            binding.dynamicColorsSwitch.isChecked = useDynamicColors
        }

        binding.dynamicColorsSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                tokenManager.saveDynamicColors(isChecked)
            }
        }
    }

    companion object {
        const val TAG = "SettingsBottomSheet"
    }
}