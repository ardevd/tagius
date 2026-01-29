package net.ardevd.tagius.features.auth.ui

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import net.ardevd.tagius.R
import net.ardevd.tagius.core.data.TokenManager
import net.ardevd.tagius.core.network.RetrofitClient
import net.ardevd.tagius.databinding.FragmentLoginBinding
import net.ardevd.tagius.features.auth.viewmodel.ErrorField
import net.ardevd.tagius.features.auth.viewmodel.LoginUiState
import net.ardevd.tagius.features.auth.viewmodel.LoginViewModel
import net.ardevd.tagius.features.background.ZombieCheckWorker
import net.ardevd.tagius.features.records.ui.list.RecordsListFragment
import java.util.concurrent.TimeUnit

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        // Hide Fab
        val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fabAdd)
        fab.isVisible = false

        // Handle Click
        binding.connectButton.setOnClickListener {
            val url = binding.urlInput.text.toString().trim()
            val token = binding.tokenInput.text.toString().trim()

            // Clear previous errors
            binding.urlInputLayout.error = null
            binding.tokenInputLayout.error = null

            if (url.isNotEmpty() && token.isNotEmpty()) {
                // Close keyboard
                val imm = requireContext().getSystemService(InputMethodManager::class.java)
                imm.hideSoftInputFromWindow(view.windowToken, 0)

                viewModel.verifyAndLogin(url, token)
            } else {
                if (url.isEmpty()) {
                    binding.urlInputLayout.error = "URL cannot be empty"
                }
                if (token.isEmpty()) {
                    binding.tokenInputLayout.error = "Token cannot be empty"
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is LoginUiState.Idle -> {
                            setLoading(false)
                        }

                        is LoginUiState.Loading -> {
                            setLoading(true)
                        }

                        is LoginUiState.Success -> {
                            setLoading(false)
                            // Proceed to save and navigate
                            performLoginSuccess()
                        }

                        is LoginUiState.Error -> {
                            setLoading(false)
                            handleError(state)
                        }
                    }
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.connectButton.isEnabled = !isLoading
        binding.connectButton.text = if (isLoading) "Verifying..." else "Connect"
    }

    private fun handleError(error: LoginUiState.Error) {
        val messageStr = error.message.asString(requireContext())

        when (error.field) {
            ErrorField.URL -> binding.urlInputLayout.error = messageStr
            ErrorField.TOKEN -> binding.tokenInputLayout.error = messageStr
            ErrorField.GENERAL -> Toast.makeText(context, messageStr, Toast.LENGTH_LONG).show()
        }
    }

    private fun performLoginSuccess() {
        val url = binding.urlInput.text.toString().trim()
        val token = binding.tokenInput.text.toString().trim()

        // Launch coroutine for saving
        viewLifecycleOwner.lifecycleScope.launch {
            TokenManager(requireContext()).saveConnectionDetails(url, token)
            RetrofitClient.reset()

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, RecordsListFragment())
                .commit()

            requireActivity().findViewById<View>(R.id.topAppBar).isVisible = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}