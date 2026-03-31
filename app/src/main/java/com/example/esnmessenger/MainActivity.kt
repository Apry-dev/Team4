package com.example.esnmessenger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.esnmessenger.navigation.NavGraph
import com.example.esnmessenger.ui.theme.ESNMessengerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ESNMessengerTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}
