package net.ardevd.tagius.features.settings.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.ardevd.tagius.BuildConfig
import androidx.lifecycle.lifecycleScope
import androidx.fragment.app.setFragmentResult
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ardevd.tagius.core.data.TokenManager
import net.ardevd.tagius.databinding.FragmentSettingsBinding

class SettingsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show version info
        val version = BuildConfig.VERSION_NAME
        binding.versionText.text = "v$version"

        // Display the stored URL so the user knows which server they are on
        viewLifecycleOwner.lifecycleScope.launch {
            val url = TokenManager(requireContext()).serverUrlFlow.first()
            binding.serverUrlText.text = url
        }

        // Handle Logout
        binding.logoutButton.setOnClickListener {
            dismiss()
            setFragmentResult(REQUEST_LOGOUT, bundleOf())
        }

        // Dynamic Colors toggle
        val tokenManager = TokenManager(requireContext())
        
        val listener = android.widget.CompoundButton.OnCheckedChangeListener { _, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                tokenManager.setDynamicColorsEnabled(isChecked)
                requireActivity().recreate()
            }
        }
        
        binding.dynamicColorsSwitch.isEnabled = false
        binding.dynamicColorsSwitch.setOnCheckedChangeListener(listener)

        viewLifecycleOwner.lifecycleScope.launch {
            val isEnabled = tokenManager.dynamicColorsFlow.first()
            binding.dynamicColorsSwitch.setOnCheckedChangeListener(null)
            binding.dynamicColorsSwitch.isChecked = isEnabled
            binding.dynamicColorsSwitch.setOnCheckedChangeListener(listener)
            binding.dynamicColorsSwitch.isEnabled = true
        }
    }

    companion object {
        const val TAG = "SettingsBottomSheet"
        const val REQUEST_LOGOUT = "request_logout"
    }
}