package com.samai.assistant.utils

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun isAccessibilityEnabled(): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any {
            it.resolveInfo.serviceInfo.packageName == context.packageName
        }
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun getPermissionExplanation(permission: String): String {
        return when (permission) {
            "accessibility" -> "SAM needs Accessibility Service to interact with apps on your behalf. This allows tapping, typing, and scrolling when you ask SAM to perform actions. SAM only acts on your commands."
            "microphone" -> "SAM uses the microphone for voice commands. When you speak to SAM, audio is processed locally for speech recognition."
            "camera" -> "SAM can launch the camera app when you ask to take a photo or scan something."
            "notifications" -> "SAM may need notification access to show task status and voice activation notifications."
            else -> "This permission helps SAM assist you more effectively."
        }
    }
}
