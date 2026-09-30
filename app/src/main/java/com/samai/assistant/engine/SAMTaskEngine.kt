package com.samai.assistant.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import com.samai.assistant.androidcontrol.AndroidControlEngine
import com.samai.assistant.overlay.OverlayState
import com.samai.assistant.overlay.SAMOverlayManager

data class TaskResult(val success: Boolean, val message: String)

class SAMTaskEngine(
    private val context: Context,
    private val overlay: SAMOverlayManager
) {
    suspend fun executeCommand(command: String): TaskResult {
        val lower = command.lowercase().trim()

        return when {
            lower.contains("open") && lower.contains("whatsapp") -> openWhatsApp()
            lower.contains("open") && lower.contains("instagram") -> openApp("com.instagram.android", "Instagram")
            lower.contains("open") && lower.contains("youtube") -> openYouTubeAndSearch(lower)
            lower.contains("open") && lower.contains("chrome") || lower.contains("google") && lower.contains("search") -> openGoogleSearch(lower)
            lower.contains("open") && lower.contains("maps") -> openMaps(lower)
            lower.contains("open") && lower.contains("gmail") -> openApp("com.google.android.gm", "Gmail")
            lower.contains("open") && lower.contains("telegram") -> openApp("org.telegram.messenger", "Telegram")
            lower.contains("set") && lower.contains("alarm") -> setAlarm(lower)
            lower.contains("wake") && lower.contains("at") -> setAlarm(lower)
            lower.contains("open") && lower.contains("settings") -> openSettings()
            lower.contains("open") && lower.contains("camera") -> openCamera()
            lower.contains("message") && lower.contains("whatsapp") -> sendWhatsAppMessage(lower)
            lower.contains("call") -> initiateCall(lower)
            lower.contains("open") -> parseGenericOpen(lower)
            else -> TaskResult(false, "I couldn't understand that command. Try: 'Open [app]', 'Set alarm for [time]', or 'Search Google for [query]'")
        }
    }

    private fun openWhatsApp(): TaskResult {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                verifyLaunch("com.whatsapp", "WhatsApp")
            } else {
                TaskResult(false, "WhatsApp is not installed on this device.")
            }
        } catch (e: Exception) {
            TaskResult(false, "Couldn't open WhatsApp: ${e.message}")
        }
    }

    private fun openApp(packageName: String, appName: String): TaskResult {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                verifyLaunch(packageName, appName)
            } else {
                TaskResult(false, "$appName is not installed.")
            }
        } catch (e: Exception) {
            TaskResult(false, "Couldn't open $appName: ${e.message}")
        }
    }

    private fun openYouTubeAndSearch(command: String): TaskResult {
        return try {
            val searchQuery = extractSearchAfter(command, listOf("search", "for", "search for"))
            if (searchQuery != null) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(searchQuery)}"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                TaskResult(true, "Searching YouTube for '$searchQuery'.")
            } else {
                val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    TaskResult(true, "Opened YouTube.")
                } else {
                    TaskResult(false, "YouTube is not installed.")
                }
            }
        } catch (e: Exception) {
            TaskResult(false, "Couldn't open YouTube: ${e.message}")
        }
    }

    private fun openGoogleSearch(command: String): TaskResult {
        return try {
            val query = extractSearchAfter(command, listOf("search", "for", "google", "search google for", "search for"))
                ?: command.replace("open", "").replace("google", "").replace("and", "").replace("search", "").replace("for", "").trim()

            if (query.isNotBlank()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                TaskResult(true, "Searching Google for '$query'.")
            } else {
                val intent = context.packageManager.getLaunchIntentForPackage("com.android.chrome")
                    ?: context.packageManager.getLaunchIntentForPackage("com.google.android.apps.search")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    TaskResult(true, "Opened Google search.")
                } else {
                    TaskResult(false, "No browser found.")
                }
            }
        } catch (e: Exception) {
            TaskResult(false, "Search failed: ${e.message}")
        }
    }

    private fun openMaps(command: String): TaskResult {
        return try {
            val location = command.replace("open", "").replace("maps", "").replace("google", "").replace("navigate", "").replace("to", "").trim()
            val uri = if (location.isNotBlank()) {
                "https://www.google.com/maps/search/?api=1&query=${Uri.encode(location)}"
            } else {
                "https://www.google.com/maps"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            TaskResult(true, if (location.isNotBlank()) "Opening Maps for '$location'." else "Opening Google Maps.")
        } catch (e: Exception) {
            TaskResult(false, "Couldn't open Maps: ${e.message}")
        }
    }

    private fun setAlarm(command: String): TaskResult {
        return try {
            val hourPattern = Regex("""(\d{1,2})[:\s]?(\d{2})?\s*(am|pm|a\.m\.|p\.m\.)?""", RegexOption.IGNORE_CASE)
            val match = hourPattern.find(command)

            if (match != null) {
                var hour = match.groupValues[1].toIntOrNull() ?: 7
                val minute = match.groupValues[2].toIntOrNull() ?: 0
                val amPm = match.groupValues[3].lowercase().trimStart('.', ' ')

                if (amPm.startsWith("pm") && hour < 12) hour += 12
                if (amPm.startsWith("am") && hour == 12) hour = 0

                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    putExtra(AlarmClock.EXTRA_MESSAGE, "SAM Alarm")
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    val timeStr = formatTime(hour, minute)
                    TaskResult(true, "Alarm set for $timeStr.")
                } else {
                    TaskResult(false, "No alarm app found on this device.")
                }
            } else {
                TaskResult(false, "I couldn't understand the alarm time. Try 'Set alarm for 7 AM'.")
            }
        } catch (e: Exception) {
            TaskResult(false, "Couldn't set alarm: ${e.message}")
        }
    }

    private fun sendWhatsAppMessage(command: String): TaskResult {
        return try {
            val phonePattern = Regex("""(\+?\d[\d\s-]{7,})""")
            val phoneMatch = phonePattern.find(command)

            val message = extractAfter(command, "saying", "message", "type")

            if (phoneMatch != null) {
                val number = phoneMatch.groupValues[1].replace(" ", "").replace("-", "")
                val uri = if (message != null && message.isNotBlank()) {
                    "https://api.whatsapp.com/send?phone=$number&text=${Uri.encode(message)}"
                } else {
                    "https://api.whatsapp.com/send?phone=$number"
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                TaskResult(true, "Opening WhatsApp to message $number. Please confirm before sending.")
            } else {
                val name = extractAfter(command, "message", "to")
                TaskResult(true, "Opening WhatsApp. Please find ${name ?: "the contact"} and type your message. Confirm before sending.")
            }
        } catch (e: Exception) {
            TaskResult(false, "Couldn't prepare WhatsApp message: ${e.message}")
        }
    }

    private fun initiateCall(command: String): TaskResult {
        return try {
            val phonePattern = Regex("""(\+?\d[\d\s-]{7,})""")
            val match = phonePattern.find(command)
            if (match != null) {
                val number = match.groupValues[1]
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                TaskResult(true, "Opening dialer for $number. Please confirm the call.")
            } else {
                TaskResult(false, "I need a phone number to make a call.")
            }
        } catch (e: Exception) {
            TaskResult(false, "Call failed: ${e.message}")
        }
    }

    private fun openSettings(): TaskResult {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            TaskResult(true, "Opened Settings.")
        } catch (e: Exception) {
            TaskResult(false, "Couldn't open Settings.")
        }
    }

    private fun openCamera(): TaskResult {
        return try {
            val intent = Intent("android.media.action.STILL_IMAGE_CAMERA")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            TaskResult(true, "Opened Camera.")
        } catch (e: Exception) {
            TaskResult(false, "Couldn't open Camera.")
        }
    }

    private fun parseGenericOpen(command: String): TaskResult {
        val appName = command.replace("open", "").replace("the", "").replace("app", "").trim()
        if (appName.isBlank()) return TaskResult(false, "Which app should I open?")

        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(appName)
            ?: run {
                val apps = pm.getInstalledApplications(0)
                val match = apps.find {
                    pm.getApplicationLabel(it).toString().lowercase().contains(appName.lowercase())
                }
                match?.packageName?.let { pm.getLaunchIntentForPackage(it) }
            }

        return if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            TaskResult(true, "Opened $appName.")
        } else {
            TaskResult(false, "I couldn't find '$appName' on this device.")
        }
    }

    private fun verifyLaunch(packageName: String, appName: String): TaskResult {
        val pm = context.packageManager
        return try {
            pm.getPackageInfo(packageName, 0)
            TaskResult(true, "$appName opened successfully.")
        } catch (_: Exception) {
            TaskResult(false, "$appName might not have opened correctly.")
        }
    }

    private fun extractSearchAfter(text: String, keywords: List<String>): String? {
        for (keyword in keywords.sortedByDescending { it.length }) {
            val index = text.indexOf(keyword, ignoreCase = true)
            if (index >= 0) {
                val after = text.substring(index + keyword.length).trim()
                if (after.isNotBlank()) return after.removePrefix("for").removePrefix("for").trim()
            }
        }
        return null
    }

    private fun extractAfter(text: String, vararg markers: String): String? {
        for (marker in markers) {
            val idx = text.indexOf(marker, ignoreCase = true)
            if (idx >= 0) {
                return text.substring(idx + marker.length).trim()
            }
        }
        return null
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val period = if (hour >= 12) "PM" else "AM"
        val displayHour = if (hour % 12 == 0) 12 else hour % 12
        return if (minute == 0) "$displayHour $period" else "$displayHour:${minute.toString().padStart(2, '0')} $period"
    }
}
