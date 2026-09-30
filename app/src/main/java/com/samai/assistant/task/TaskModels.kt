package com.samai.assistant.task

enum class TaskState {
    PENDING,
    PLANNING,
    RUNNING,
    WAITING_PERMISSION,
    WAITING_CONFIRMATION,
    VERIFYING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class ExecutionStep(
    val id: String,
    val description: String,
    val action: String,
    val params: Map<String, String> = emptyMap(),
    val state: TaskState = TaskState.PENDING,
    val requiresConfirmation: Boolean = false,
    val verifyAfter: Boolean = true
)

data class Task(
    val id: String,
    val title: String,
    val userCommand: String,
    val steps: List<ExecutionStep>,
    val currentState: TaskState = TaskState.PENDING,
    val currentStepIndex: Int = 0,
    val result: String = "",
    val error: String? = null
)
package com.samai.assistant.task

enum class TaskState {
    PENDING,
    RUNNING,
    WAITING_FOR_USER,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class ExecutableTask(
    val id: String,
    val description: String,
    val steps: List<TaskStepExecution>,
    var currentState: TaskState = TaskState.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

data class TaskStepExecution(
    val stepId: Int,
    val action: String,
    val target: String,
    val parameters: Map<String, String> = emptyMap(),
    val description: String,
    var state: TaskState = TaskState.PENDING,
    var result: String? = null,
    var error: String? = null
)

data class TaskResult(
    val taskId: String,
    val success: Boolean,
    val message: String,
    val failedStep: Int? = null,
    val failureReason: String? = null
)
