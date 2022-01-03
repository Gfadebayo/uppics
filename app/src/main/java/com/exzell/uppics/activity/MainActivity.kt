package com.exzell.uppics.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.View
import com.exzell.uppics.R
import com.exzell.uppics.UserManager
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

                if(dest.id == R.id.frag_post) toolbarLayout.setExpanded(true, false)

                twinFab.root.visibility = if(dest.id == R.id.frag_home) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        val dest = findNavController(R.id.nav_host_fragment).currentDestination?.id
        if(dest == R.id.frag_login || dest == R.id.frag_home) finish()
        else super.onBackPressed()
    }
}