package com.yid.app.feature.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings

// The Android settings page Yiḍ sends the reader to, from Settings and from the welcome guide, ported from LinkedOut's SystemPages.kt.
// not ported: xLinksEnabled (X's link state: Yiḍ's is blueskyLinksEnabled, in SettingsScreen.kt)
// not ported: openLinkSettings (Yiḍ keeps its own copy in SettingsScreen.kt)
// not ported: openBatterySettings (LinkedOut's file has none: a refused battery exemption asks again through BatteryExemption.request)

/**
 * Yiḍ's own notification page in Android settings: ACTION_APP_NOTIFICATION_SETTINGS with EXTRA_APP_PACKAGE, with
 * the new task flag kept, since a non-activity context may call it.
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/settings/SystemPages.kt#openNotificationSettings
 */
internal fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
