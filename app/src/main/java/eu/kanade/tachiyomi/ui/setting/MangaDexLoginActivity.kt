package eu.kanade.tachiyomi.ui.setting

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.ui.base.activity.BaseThemedActivity
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.withUIContext
import org.nekomanga.R
import org.nekomanga.presentation.screens.LoadingScreen
import org.nekomanga.presentation.theme.NekoTheme
import uy.kohesive.injekt.injectLazy

/** Receives the MangaDex OAuth redirect and trades its code for session tokens. */
class MangaDexLoginActivity : BaseThemedActivity() {

    private val loginHelper: MangaDexLoginHelper by injectLazy()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NekoTheme { LoadingScreen() } }

        val code = intent.data?.getQueryParameter("code")
        if (code == null) {
            loginHelper.invalidate()
            returnToSettings()
            return
        }
        lifecycleScope.launchIO {
            if (!loginHelper.login(code)) {
                withUIContext { applicationContext.toast(R.string.could_not_sign_in) }
            }
            returnToSettings()
        }
    }

    private fun returnToSettings() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finishAfterTransition()
    }
}
