package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.viewmodel.MainActivity

object ShortcutUtils {
    fun addMainShortcut(context: Context) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        if (shortcutManager?.isRequestPinShortcutSupported == false) return

        val shortcut = ShortcutInfoCompat.Builder(context, "main_shortcut")
            .setShortLabel(context.getString(R.string.app_name))
            .setLongLabel(context.getString(R.string.app_name))
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_launcher_foreground))
            .setIntent(Intent(context, MainActivity::class.java).setAction(Intent.ACTION_VIEW))
            .build()

        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
    }
}
