package com.example.mosim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.mosim.navigation.MosimNavHost
import com.example.mosim.ui.theme.MosimTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MosimTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MosimNavHost()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        AppVisibility.isVisible = true
    }

    override fun onStop() {
        AppVisibility.isVisible = false
        super.onStop()
    }
}
