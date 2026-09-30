# SAM AI ProGuard Rules
# 0x53686C6F6B

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature
-keepattributes *Annotation*

-keep class com.samai.assistant.ai.** { *; }
-keep class com.samai.assistant.memory.MemoryEntry { *; }
-keep class com.samai.assistant.task.** { *; }

-keepclassmembers class * extends android.accessibilityservice.AccessibilityService { *; }
