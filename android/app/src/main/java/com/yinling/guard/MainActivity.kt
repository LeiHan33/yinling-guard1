package com.yinling.guard

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.yinling.guard.data.ServiceLocator
import com.yinling.guard.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val onboardingDestinations = setOf(
        R.id.onboardingWelcomeFragment,
        R.id.onboardingIntroFragment,
        R.id.onboardingPermissionFragment,
        R.id.onboardingDoneFragment
    )

    private val secondaryDestinations = setOf(
        R.id.helpFragment,
        R.id.familyHomeFragment,
        R.id.keywordListFragment,
        R.id.blacklistFragment,
        R.id.whitelistFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceLocator.init(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHost.navController
        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.visibility = when (destination.id) {
                in onboardingDestinations, in secondaryDestinations -> View.GONE
                else -> View.VISIBLE
            }
        }

        val config = ServiceLocator.repository(this).loadConfig()
        if (!config.onboardingCompleted) {
            navController.navigate(R.id.onboardingWelcomeFragment)
        }
    }
}
