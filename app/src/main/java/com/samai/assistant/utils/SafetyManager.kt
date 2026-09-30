package com.samai.assistant.utils

import javax.inject.Inject
import javax.inject.Singleton

enum class RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }

data class SafetyCheck(
    val action: String,
    val riskLevel: RiskLevel,
    val requiresConfirmation: Boolean,
    val confirmationMessage: String
)

@Singleton
class SafetyManager @Inject constructor() {
    private val highRiskKeywords = listOf(
        "send", "delete", "purchase", "transfer", "post", "publish",
        "remove account", "change password", "unsubscribe"
    )

    private val mediumRiskKeywords = listOf(
        "message", "email", "upload", "download", "install", "share"
    )

    fun checkAction(action: String, target: String): SafetyCheck {
        val combined = "$action $target".lowercase()

        val riskLevel = when {
            highRiskKeywords.any { combined.contains(it) } -> RiskLevel.CRITICAL
            mediumRiskKeywords.any { combined.contains(it) } -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        val requiresConfirmation = riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL

        val confirmationMsg = when (riskLevel) {
            RiskLevel.CRITICAL -> "This action may have significant consequences. Should I continue?"
            RiskLevel.HIGH -> "This action will make changes. Should I proceed?"
            RiskLevel.MEDIUM -> "I'm about to perform this action. Continue?"
            RiskLevel.LOW -> ""
        }

        return SafetyCheck(action, riskLevel, requiresConfirmation, confirmationMsg)
    }
}
