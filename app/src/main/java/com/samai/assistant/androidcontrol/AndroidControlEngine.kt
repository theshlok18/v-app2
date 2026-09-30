package com.samai.assistant.androidcontrol

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.media.AudioManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidControlEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val accessibilityService: SAMAccessibilityService?
        get() = AccessibilityServiceHolder.getService()

    suspend fun executeAction(action: String, target: String, parameters: Map<String, String>): Pair<Boolean, String> {
        return try {
            when (action) {
                "launch_app" -> launchApp(target)
                "go_home" -> goHome()
                "go_back" -> goBack()
                "recent_apps" -> openRecentApps()
                "tap_element" -> tapElement(target, parameters)
                "long_press" -> longPressElement(target, parameters)
                "swipe" -> performSwipe(parameters)
                "scroll" -> performScroll(target, parameters)
                "type_text" -> typeText(target, parameters)
                "select_element" -> selectElement(target, parameters)
                "read_screen" -> readScreen()
                "open_notification" -> openNotifications()
                "adjust_volume" -> adjustVolume(target, parameters)
                "open_camera" -> openCamera()
                "open_browser" -> openBrowser(target)
                "open_maps" -> openMaps(target)
                "media_control" -> mediaControl(target)
                "wait_ui" -> waitForUI(parameters)
                "find_element" -> findElement(target, parameters)
                "submit_search" -> submitSearch(target, parameters)
                else -> Pair(false, "Unknown action: $action")
            }
        } catch (e: Exception) {
            Pair(false, "Action failed: ${e.message}")
        }
    }

    /**
     * Convenience method for TaskPlanner execution with Map params.
     * Extracts target from params or defaults to empty.
     */
    suspend fun executeAction(action: String, parameters: Map<String, String>): String? {
        val target = parameters["target"] ?: parameters["text"] ?: parameters["package"] ?: ""
        val result = executeAction(action, target, parameters)
        return if (result.first) result.second else "Error: ${result.second}"
    }

    private fun launchApp(packageName: String): Pair<Boolean, String> {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Pair(true, "Launched $packageName")
            } else {
                Pair(false, "App not found: $packageName. It may not be installed.")
            }
        } catch (e: Exception) {
            Pair(false, "Could not launch app: ${e.message}")
        }
    }

    private fun goHome(): Pair<Boolean, String> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return Pair(true, "Returned to Home")
    }

    private fun goBack(): Pair<Boolean, String> {
        return if (accessibilityService != null) {
            accessibilityService!!.performBack()
            Pair(true, "Pressed Back")
        } else {
            Pair(false, "Accessibility service not enabled. Please enable it in Settings.")
        }
    }

    private fun openRecentApps(): Pair<Boolean, String> {
        return if (accessibilityService != null) {
            accessibilityService!!.performRecentApps()
            Pair(true, "Opened Recent Apps")
        } else {
            Pair(false, "Accessibility service not enabled.")
        }
    }

    private fun tapElement(target: String, params: Map<String, String>): Pair<Boolean, String> {
        return if (accessibilityService != null) {
            val result = accessibilityService!!.performClick(target, params)
            Pair(result.first, result.second)
        } else {
            Pair(false, "Accessibility service required for UI interaction. Enable in Settings > Accessibility.")
        }
    }

    private fun longPressElement(target: String, params: Map<String, String>): Pair<Boolean, String> {
        return if (accessibilityService != null) {
            val result = accessibilityService!!.performLongPress(target, params)
            Pair(result.first, result.second)
        } else {
            Pair(false, "Accessibility service required.")
        }
    }

    private fun performSwipe(params: Map<String, String>): Pair<Boolean, String> {
        return if (accessibilityService != null) {
            val result = accessibilityService!!.performSwipe(params)
            Pair(result.first, result.second)
        } else {
            Pair(false, "Accessibility service required.")
        }
    }

    private fun performScroll(direction: String, params: Map<String, String>): Pair<Boolean, String> {
        return if (accessibilityService != null) {
            val result = accessibilityService!!.performScroll(direction, params)
            Pair(result.first, result.second)
        } else {
            Pair(false, "Accessibility service required.")
        }
    }

    private fun typeText(text: String, params: Map<String, String>): Pair<Boolean, String> {
        return if (accessibilityService != null) {
            val result = accessibilityService!!.performTypeText(text, params)
            Pair(result.first, result.second)
        } else {
            Pair(false, "Accessibility service required for text input.")
        }
    }

    private fun selectElement(target: String, params: Map<String, String>): Pair<Boolean, String> {
        return if (accessibilityService != null) {
            val result = accessibilityService!!.performSelect(target, params)
            Pair(result.first, result.second)
        } else {
            Pair(false, "Accessibility service required.")
        }
    }

    private fun readScreen(): Pair<Boolean, String> {
        return if (accessibilityService != null) {
            val content = accessibilityService!!.readVisibleContent()
            Pair(content.isNotEmpty(), content)
        } else {
            Pair(false, "Accessibility service required to read screen content.")
        }
    }

    private fun openNotifications(): Pair<Boolean, String> {
        return if (accessibilityService != null) {
            accessibilityService!!.openNotifications()
            Pair(true, "Opened notifications")
        } else {
            Pair(false, "Accessibility service required.")
        }
    }

    private fun adjustVolume(direction: String, params: Map<String, String>): Pair<Boolean, String> {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            when (direction) {
                "up" -> audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                "down" -> audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                "mute" -> audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
                "unmute" -> audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI)
            }
            Pair(true, "Volume adjusted: $direction")
        } catch (e: Exception) {
            Pair(false, "Volume control failed: ${e.message}")
        }
    }

    private fun openCamera(): Pair<Boolean, String> {
        return try {
            val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Pair(true, "Camera opened")
            } else {
                Pair(false, "No camera app available")
            }
        } catch (e: Exception) {
            Pair(false, "Could not open camera: ${e.message}")
        }
    }

    private fun openBrowser(url: String): Pair<Boolean, String> {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Pair(true, "Opened browser with $url")
        } catch (e: Exception) {
            Pair(false, "Could not open browser: ${e.message}")
        }
    }

    private fun openMaps(location: String): Pair<Boolean, String> {
        return try {
            val uri = Uri.parse("geo:0,0?q=${Uri.encode(location)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Pair(true, "Opening Maps for: $location")
        } catch (e: Exception) {
            Pair(false, "Could not open Maps: ${e.message}")
        }
    }

    private fun mediaControl(action: String): Pair<Boolean, String> {
        return try {
            val intent = when (action) {
                "play" -> Intent(Intent.ACTION_MEDIA_BUTTON)
                "pause" -> Intent(Intent.ACTION_MEDIA_BUTTON)
                "next" -> Intent("Music.PLAY_NEXT")
                "previous" -> Intent("Music.PLAY_PREVIOUS")
                else -> return Pair(false, "Unknown media action: $action")
            }
            context.sendBroadcast(intent)
            Pair(true, "Media control: $action")
        } catch (e: Exception) {
            Pair(false, "Media control failed: ${e.message}")
        }
    }

    private suspend fun waitForUI(params: Map<String, String>): Pair<Boolean, String> {
        val timeout = params["timeout_ms"]?.toLongOrNull() ?: 3000L
        kotlinx.coroutines.delay(timeout)
        return Pair(true, "Waited for UI")
    }

    private fun findElement(target: String, params: Map<String, String>): Pair<Boolean, String> {
        return if (accessibilityService != null) {
            val result = accessibilityService!!.findElement(target, params)
            Pair(result.first, result.second)
        } else {
            Pair(false, "Accessibility service required.")
        }
    }

    private fun submitSearch(query: String, params: Map<String, String>): Pair<Boolean, String> {
        return if (accessibilityService != null) {
            val result = accessibilityService!!.performSearchSubmit(query, params)
            Pair(result.first, result.second)
        } else {
            Pair(false, "Accessibility service required.")
        }
    }
}
