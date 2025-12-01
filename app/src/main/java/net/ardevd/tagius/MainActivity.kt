package net.ardevd.tagius

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ardevd.tagius.core.data.TokenManager
import net.ardevd.tagius.databinding.ActivityMainBinding
import net.ardevd.tagius.features.auth.ui.LoginFragment
import net.ardevd.tagius.features.records.ui.add.AddRecordBottomSheet
import net.ardevd.tagius.features.records.ui.list.RecordsListFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

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
    }
}
