package top.nihoru.app.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import top.nihoru.app.R
import top.nihoru.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("nihoru_prefs", MODE_PRIVATE)
        applyTheme()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Setup drawer navigation
        binding.navigationView.setupWithNavController(navController)

        // Hamburger menu
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }

        // Theme toggle
        binding.btnTheme.setOnClickListener {
            toggleTheme()
        }

        // Close drawer button in header
        binding.navigationView.getHeaderView(0)
            ?.findViewById<android.widget.ImageButton>(R.id.btn_close_drawer)
            ?.setOnClickListener {
                binding.drawerLayout.closeDrawer(GravityCompat.END)
            }

        // Navigate on drawer item click
        binding.navigationView.setNavigationItemSelectedListener { item ->
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            when (item.itemId) {
                R.id.nav_home -> {
                    navController.navigate(R.id.homeFragment)
                    true
                }
                R.id.nav_text_to_mal -> {
                    navController.navigate(R.id.converterFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun applyTheme() {
        val isDark = prefs.getBoolean("dark_mode", true)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun toggleTheme() {
        val isDark = prefs.getBoolean("dark_mode", true)
        val newDark = !isDark
        prefs.edit().putBoolean("dark_mode", newDark).apply()
        AppCompatDelegate.setDefaultNightMode(
            if (newDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}
