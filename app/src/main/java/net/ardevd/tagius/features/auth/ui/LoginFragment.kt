package net.ardevd.tagius.features.auth.ui
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import net.ardevd.tagius.R
import net.ardevd.tagius.core.data.TokenManager
import net.ardevd.tagius.core.network.RetrofitClient
import net.ardevd.tagius.databinding.FragmentLoginBinding
import net.ardevd.tagius.features.records.ui.list.RecordsListFragment

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        binding.connectButton.setOnClickListener {
            val url = binding.urlInput.text.toString().trim()
            val token = binding.tokenInput.text.toString().trim()

            if (validate(url, token)) {
                saveAndProceed(url, token)
            }
        }
    }

    private fun validate(url: String, token: String): Boolean {
        if (url.isEmpty()) {
            binding.urlInputLayout.error = "Required"
            return false
        }
        if (token.isEmpty()) {
            binding.tokenInputLayout.error = "Required"
            return false
        }
        return true
    }

    private fun saveAndProceed(url: String, token: String) {
        // 1. Save details
        TokenManager(requireContext()).saveConnectionDetails(url, token)

        // 2. Reset Retrofit (so it picks up the new URL)
        RetrofitClient.reset()

        // 3. Navigate to Main Screen
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, RecordsListFragment())
            .commit()

        // 4. Show FAB again (since we hid it in MainActivity logic ideally,
        // but if you are just replacing fragments, the FAB might still be there.
        // Usually, the FAB belongs to the RecordsFragment, so it will appear automatically)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        false.also { requireActivity().findViewById<View>(R.id.topAppBar).isVisible = false }
    }
}