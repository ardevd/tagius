package net.ardevd.tagius

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ardevd.tagius.core.data.TokenManager
import net.ardevd.tagius.databinding.ActivityMainBinding
import net.ardevd.tagius.features.auth.ui.LoginFragment
import net.ardevd.tagius.features.records.ui.list.RecordsListFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private fun handleIntent(intent: Intent?) {

        // Check for Share Intent
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = extractSharedText(intent)
            if (sharedText.isNotBlank()) {
                triggerAddSheet(intent, description = sharedText)
            }
        }
    }

    // Helper to combine Subject + Text (useful for Chrome which sends Title + URL)
    private fun extractSharedText(intent: Intent): String {
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: ""

        return if (subject.isNotEmpty()) {
            if (text.isNotEmpty()) {
                "$subject $text"
            } else {
                subject
            }
        } else {
            text
        }
    }

    private fun triggerAddSheet(intent: Intent, description: String?) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as? RecordsListFragment

        // Logic: Pass the description payload
        if (fragment != null && fragment.isAdded && fragment.view != null) {
            // Warm Start
            description?.let { fragment.openAddSheet(it) }
        } else {
            // Cold Start
            intent.putExtra("PENDING_OPEN_SHEET", true)
            if (description != null) {
                intent.putExtra("PENDING_DESCRIPTION", description)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppBar)
        val tokenManager = TokenManager(this)
        lifecycleScope.launch {
            val token = tokenManager.authTokenFlow.first()
            if (savedInstanceState == null) {
                if (!token.isNullOrBlank()) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, RecordsListFragment())
                        .commit()
                } else {
                    // Not logged in? Go to Login
                    binding.topAppBar.isVisible = false
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LoginFragment())
                        .commit()
                }
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }
}
