package com.exzell.uppics

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.View
import com.exzell.uppics.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        UserManager.init()

        binding = ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(root)

            setSupportActionBar(toolbar)

            val navController = findNavController(R.id.nav_host_fragment)

            appBarConfiguration = AppBarConfiguration(setOf(R.id.frag_home, R.id.frag_login))

            setupActionBarWithNavController(navController, appBarConfiguration)

            navController.addOnDestinationChangedListener { controller, dest, arguments ->
                toolbar.visibility = if(dest.id == R.id.frag_login) View.GONE else View.VISIBLE

                twinFab.root.visibility = if(dest.id == R.id.frag_home) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}