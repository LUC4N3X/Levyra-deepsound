package com.luc4n3x.levyra.diagnostics

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.luc4n3x.levyra.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LevyraLauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val pending = withContext(Dispatchers.IO) {
                runCatching { CrashReporting.store(this@LevyraLauncherActivity).claimPending() }.getOrNull()
            }
            val next = if (pending != null) {
                Intent(this@LevyraLauncherActivity, CrashReportsActivity::class.java)
                    .putExtra(CrashReportsActivity.EXTRA_REPORT_ID, pending.id)
                    .putExtra(CrashReportsActivity.EXTRA_CONTINUE_TO_APP, true)
            } else {
                Intent(this@LevyraLauncherActivity, MainActivity::class.java)
                    .setAction(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(next)
            finish()
        }
    }
}
