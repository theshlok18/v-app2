package com.samai.assistant.androidcontrol

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class SAMAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Process accessibility events for UI state tracking
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityServiceHolder.setService(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        AccessibilityServiceHolder.setService(null)
    }

    fun performBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun performRecentApps(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    fun openNotifications(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    fun performClick(target: String, params: Map<String, String>): Pair<Boolean, String> {
        val root = rootInActiveWindow ?: return Pair(false, "No active window found")

        val node = findNodeByTarget(root, target, params)
            ?: return Pair(false, "Element '$target' not found on screen")

        return if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            Pair(true, "Clicked on '$target'")
        } else {
            // Try clicking parent if node itself can't be clicked
            val parent = node.parent
            if (parent?.isClickable == true && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                Pair(true, "Clicked parent of '$target'")
            } else {
                Pair(false, "Element '$target' is not clickable")
            }
        }
    }

    fun performLongPress(target: String, params: Map<String, String>): Pair<Boolean, String> {
        val root = rootInActiveWindow ?: return Pair(false, "No active window")
        val node = findNodeByTarget(root, target, params)
            ?: return Pair(false, "Element '$target' not found")

        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)
        val path = Path().apply {
            moveTo(bounds.centerX.toFloat(), bounds.centerY.toFloat())
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 1000))
            .build()
        val dispatched = dispatchGesture(gesture, null, null)
        return if (dispatched) Pair(true, "Long pressed '$target'") else Pair(false, "Gesture dispatch failed")
    }

    fun performSwipe(params: Map<String, String>): Pair<Boolean, String> {
        val direction = params["direction"] ?: "up"
        val distance = params["distance"]?.toFloatOrNull() ?: 500f

        val metrics = resources.displayMetrics
        val centerX = metrics.widthPixels / 2f
        val startY = when (direction) {
            "up" -> metrics.heightPixels * 0.7f
            "down" -> metrics.heightPixels * 0.3f
            else -> metrics.heightPixels / 2f
        }
        val endY = when (direction) {
            "up" -> startY - distance
            "down" -> startY + distance
            else -> startY
        }

        val path = Path().apply {
            moveTo(centerX, startY)
            lineTo(centerX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()

        return if (dispatchGesture(gesture, null, null)) Pair(true, "Swiped $direction") else Pair(false, "Swipe failed")
    }

    fun performScroll(direction: String, params: Map<String, String>): Pair<Boolean, String> {
        val root = rootInActiveWindow ?: return Pair(false, "No active window")
        val scrollable = findScrollableNode(root)
            ?: return Pair(false, "No scrollable element found")

        val action = when (direction) {
            "down", "up" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            "up_reverse" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            else -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        }

        return if (scrollable.performAction(action)) Pair(true, "Scrolled $direction")
        else Pair(false, "Could not scroll in that direction")
    }

    fun performTypeText(text: String, params: Map<String, String>): Pair<Boolean, String> {
        val root = rootInActiveWindow ?: return Pair(false, "No active window")
        val targetField = params["field"]?.let { findNodeByTarget(root, it, params) }
            ?: root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: return Pair(false, "No text input field focused")

        val arguments = android.os.Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return if (targetField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) {
            Pair(true, "Typed: '$text'")
        } else {
            Pair(false, "Could not set text in field")
        }
    }

    fun performSelect(target: String, params: Map<String, String>): Pair<Boolean, String> {
        val root = rootInActiveWindow ?: return Pair(false, "No active window")
        val node = findNodeByTarget(root, target, params)
            ?: return Pair(false, "Element '$target' not found")

        return if (node.performAction(AccessibilityNodeInfo.ACTION_SELECT)) {
            Pair(true, "Selected '$target'")
        } else {
            Pair(false, "Cannot select element")
        }
    }

    fun readVisibleContent(): String {
        val root = rootInActiveWindow ?: return ""
        val builder = StringBuilder()
        collectText(root, builder, 0)
        return builder.toString()
    }

    fun findElement(target: String, params: Map<String, String>): Pair<Boolean, String> {
        val root = rootInActiveWindow ?: return Pair(false, "No active window")
        val node = findNodeByTarget(root, target, params)
        return if (node != null) {
            val bounds = android.graphics.Rect()
            node.getBoundsInScreen(bounds)
            Pair(true, "Found '$target' at (${bounds.left},${bounds.top},${bounds.right},${bounds.bottom})")
        } else {
            Pair(false, "'$target' not found")
        }
    }

    fun performSearchSubmit(query: String, params: Map<String, String>): Pair<Boolean, String> {
        val root = rootInActiveWindow ?: return Pair(false, "No active window")
        // First type the query
        val typeResult = performTypeText(query, params)
        if (!typeResult.first) return typeResult
        // Then press enter/search
        val focusedNode = root.findFocus(android.view.accessibility.AccessibilityNodeInfo.FOCUS_INPUT)
        if (focusedNode != null) {
            focusedNode.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_IME_ENTER)
            return Pair(true, "Submitted search: '$query'")
        }
        return Pair(true, "Search submitted")
    }

    private fun findNodeByTarget(root: AccessibilityNodeInfo, target: String, params: Map<String, String>): AccessibilityNodeInfo? {
        // Search by text
        val textNodes = root.findAccessibilityNodeInfosByText(target)
        if (textNodes.isNotEmpty()) return textNodes.first()

        // Search by view ID
        params["resource_id"]?.let { id ->
            val idNodes = root.findAccessibilityNodeInfosByViewId(id)
            if (idNodes.isNotEmpty()) return idNodes.first()
        }

        // Search by class name
        params["class"]?.let { className ->
            return findNodeByClass(root, className)
        }

        return null
    }

    private fun findNodeByClass(root: AccessibilityNodeInfo, className: String): AccessibilityNodeInfo? {
        if (root.className?.toString()?.contains(className, ignoreCase = true) == true) return root
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findNodeByClass(child, className)
            if (found != null) return found
        }
        return null
    }

    private fun findScrollableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isScrollable) return root
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findScrollableNode(child)
            if (found != null) return found
        }
        return null
    }

    private fun collectText(node: AccessibilityNodeInfo, builder: StringBuilder, depth: Int) {
        val text = node.text?.toString() ?: node.contentDescription?.toString() ?: ""
        if (text.isNotEmpty()) {
            builder.append("  ".repeat(depth)).append(text).append("\n")
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectText(it, builder, depth + 1) }
        }
    }
}

object AccessibilityServiceHolder {
    private var service: SAMAccessibilityService? = null
    fun setService(svc: SAMAccessibilityService?) { service = svc }
    fun getService(): SAMAccessibilityService? = service
    fun isAvailable(): Boolean = service != null
}
