package com.example.cardashboardtest

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.cardashboardtest.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("Navigation", "MainActivity onCreate started")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        drawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView

        Log.d("Navigation", "NavView initialized: ${navView != null}")

        // Get NavController from NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        navController = navHostFragment.navController

        Log.d("Navigation", "NavController initialized")

        // Configure the navigation drawer
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_dashboard,
                R.id.nav_navigation,
                R.id.nav_media,
                R.id.nav_settings,
                R.id.nav_logs
            ),
            drawerLayout
        )

        // Set up the ActionBar with the NavController
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        // Set up the NavigationView with the NavController
        navView.setupWithNavController(navController)

        // Set up navigation item click listener
        navView.setNavigationItemSelectedListener { menuItem ->
            Log.d("Navigation", "Menu item clicked: ${menuItem.itemId}")
            try {
                // Use direct navigation instead of actions
                navController.navigate(menuItem.itemId)
                drawerLayout.closeDrawer(GravityCompat.START)
                true
            } catch (e: Exception) {
                Log.e("Navigation", "Navigation failed", e)
                false
            }
        }

        // Debug: Log menu items
        navView.menu?.let { menu ->
            Log.d("Navigation", "Menu items count: ${menu.size()}")
            for (i in 0 until menu.size()) {
                val item = menu.getItem(i)
                Log.d("Navigation", "Menu item ${i}: id=${item.itemId}, title=${item.title}")
            }
        }

        // Debug: Log navigation view state
        Log.d("Navigation", "NavigationView is null: ${navView == null}")
        Log.d("Navigation", "NavigationView visibility: ${navView.visibility}")
        Log.d("Navigation", "NavigationView is enabled: ${navView.isEnabled}")
        Log.d("Navigation", "NavigationView menu is null: ${navView.menu == null}")
        Log.d("Navigation", "NavigationView header count: ${navView.headerCount}")

        // Debug: Log drawer layout state
        Log.d("Navigation", "DrawerLayout is null: ${drawerLayout == null}")
        Log.d("Navigation", "DrawerLayout is enabled: ${drawerLayout.isEnabled}")
        Log.d("Navigation", "DrawerLayout is open: ${drawerLayout.isDrawerOpen(GravityCompat.START)}")

        Log.d("Navigation", "Navigation setup completed")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        Log.d("Navigation", "onSupportNavigateUp called")
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        Log.d("Navigation", "onBackPressed called")
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            Log.d("Navigation", "Closing drawer")
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            Log.d("Navigation", "Calling super.onBackPressed")
            super.onBackPressed()
        }
    }
}
