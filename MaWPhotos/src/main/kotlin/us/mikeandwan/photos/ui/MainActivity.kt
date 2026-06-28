package us.mikeandwan.photos.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import dagger.hilt.android.AndroidEntryPoint
import us.mikeandwan.photos.Constants

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val requestLocalNetworkPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* granted or denied — best-effort for dev connectivity */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFinishing) {
            return
        }

        enableEdgeToEdge()

        if (Constants.REQUEST_LOCAL_NETWORK_PERMISSION && Build.VERSION.SDK_INT >= 35) {
            val permission = "android.permission.ACCESS_LOCAL_NETWORK"
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                requestLocalNetworkPermission.launch(permission)
            }
        }

        setContent {
            MawPhotosApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
