package com.yuyagioh.tcgreader.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.yuyagioh.tcgreader.app.navigation.TcgReaderApp
import com.yuyagioh.tcgreader.app.ui.theme.TcgReaderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TcgReaderTheme {
                TcgReaderApp()
            }
        }
    }
}
