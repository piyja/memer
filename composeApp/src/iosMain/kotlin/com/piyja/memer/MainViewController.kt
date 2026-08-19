import androidx.compose.ui.window.ComposeUIViewController
import com.piyja.memer.MemerApp
import com.piyja.memer.ui.theme.MemerTheme

fun MainViewController() = ComposeUIViewController {
    MemerTheme {
        MemerApp()
    }
}
