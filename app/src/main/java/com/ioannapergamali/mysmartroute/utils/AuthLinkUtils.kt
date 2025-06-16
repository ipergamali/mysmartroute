package com.ioannapergamali.mysmartroute.utils

import com.google.firebase.auth.ActionCodeSettings
import com.ioannapergamali.mysmartroute.BuildConfig

object AuthLinkUtils {
    fun buildActionCodeSettings(): ActionCodeSettings {
        return ActionCodeSettings.newBuilder()
            .setUrl("https://${BuildConfig.FIREBASE_AUTH_DOMAIN}/completeSignIn")
            .setHandleCodeInApp(true)
            .setAndroidPackageName(
                BuildConfig.APPLICATION_ID,
                true,
                null
            )
            .setDynamicLinkDomain(BuildConfig.DYNAMIC_LINK_DOMAIN)
            .build()
    }
}
