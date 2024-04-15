package com.mobilesec.govcomm.mal

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log

class DndManager(private val context: Context) {
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun setDndMode(enable: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (notificationManager.isNotificationPolicyAccessGranted) {
                val interruptionFilter = if (enable) {
                    NotificationManager.INTERRUPTION_FILTER_NONE // DND On
                } else {
                    NotificationManager.INTERRUPTION_FILTER_ALL // DND Off
                }
                notificationManager.setInterruptionFilter(interruptionFilter)
                //Log.d("DndManager", "Do Not Disturb mode set to: $enable")
            } else {
                //Log.e("DndManager", "Do Not Disturb access not granted.")
                // Notification policy access not granted. Navigate the user to the settings to allow access.
                // Show an explanatory UI to guide users to the settings screen
            }
        } else {
            //Log.e("DndManager", "Do Not Disturb mode is not supported on this version of Android.")
            // Handle the case where the user's version of Android does not support this functionality
        }
    }
}
