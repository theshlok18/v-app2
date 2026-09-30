package com.samai.assistant.task

import com.samai.assistant.ai.ModelRouter
import com.samai.assistant.ai.TaskPlanResult
import com.samai.assistant.androidcontrol.AndroidControlEngine
import com.samai.assistant.profile.UserProfileManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskPlanner @Inject constructor(
    private val modelRouter: ModelRouter,
    private val controlEngine: AndroidControlEngine,
    private val profileManager: UserProfileManager
) {
    private val _activeTasks = MutableStateFlow<List<Task>>(emptyList())
    val activeTasks: StateFlow<List<Task>> = _activeTasks.asStateFlow()

    suspend fun planAndExecute(command: String): TaskResult {
        val userName = profileManager.getUserName()
        val taskId = "task_${System.currentTimeMillis()}"

        // Phase 1: Planning
        val planningTask = Task(
            id = taskId,
            title = command,
            userCommand = command,
            steps = emptyList(),
            currentState = TaskState.PLANNING
        )
        updateTask(planningTask)

        // Get AI plan
        val plan: TaskPlanResult = try {
            modelRouter.planTask(command)
        } catch (e: Exception) {
            val failedTask = planningTask.copy(currentState = TaskState.FAILED, error = e.message)
            updateTask(failedTask)
            return TaskResult(false, "Sorry $userName, I couldn't plan that task. ${e.message ?: "AI provider error."}")
        }

        if (plan.steps.isEmpty()) {
            val failedTask = planningTask.copy(currentState = TaskState.FAILED, error = "No steps generated")
            updateTask(failedTask)
            return TaskResult(false, "I couldn't break that command into steps.")
        }

        // Phase 2: Execute with verification
        val taskSteps = plan.steps.mapIndexed { index, step ->
            ExecutionStep(
                id = "${taskId}_$index",
                description = step.description,
                action = step.action,
                params = step.parameters + mapOf("target" to step.target),
                requiresConfirmation = step.action.lowercase() in listOf("delete", "send_message", "purchase")
            )
        }

        val executionTask = planningTask.copy(
            steps = taskSteps,
            currentState = TaskState.RUNNING
        )
        updateTask(executionTask)

        // Execute each step
        for ((index, step) in taskSteps.withIndex()) {
            if (step.requiresConfirmation) {
                val confirmTask = executionTask.copy(
                    currentState = TaskState.WAITING_CONFIRMATION,
                    currentStepIndex = index
                )
                updateTask(confirmTask)
                // In production, would wait for user confirmation
                delay(1000)
            }

            val runningTask = executionTask.copy(currentStepIndex = index, currentState = TaskState.RUNNING)
            updateTask(runningTask)

            val stepResult = try {
                controlEngine.executeAction(step.action, step.params)
            } catch (e: Exception) {
                null
            }

            // Phase 3: Verification
            if (step.verifyAfter && stepResult != null) {
                val verifyTask = executionTask.copy(currentState = TaskState.VERIFYING, currentStepIndex = index)
                updateTask(verifyTask)
                delay(500) // Allow UI to settle
            }

            if (stepResult == null || stepResult.startsWith("Error")) {
                val failedTask = executionTask.copy(
                    currentState = TaskState.FAILED,
                    currentStepIndex = index,
                    error = stepResult ?: "Step execution returned null"
                )
                updateTask(failedTask)
                return TaskResult(false, "Sorry $userName, step '${step.description}' failed. ${stepResult ?: "Unknown error."}")
            }
        }

        // Complete
        val completedTask = executionTask.copy(currentState = TaskState.COMPLETED, result = plan.summary)
        updateTask(completedTask)
        return TaskResult(true, "Done, $userName.")
    }

    private fun updateTask(task: Task) {
        val current = _activeTasks.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.id == task.id }
        if (existingIndex >= 0) current[existingIndex] = task else current.add(task)
        _activeTasks.value = current
    }
}

data class TaskResult(val success: Boolean, val message: String)
package com.samai.assistant.task

import com.samai.assistant.ai.*
import com.samai.assistant.androidcontrol.AndroidControlEngine
import com.samai.assistant.profile.UserProfileManager
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskPlanner @Inject constructor(
    private val modelRouter: ModelRouter,
    private val androidControl: AndroidControlEngine,
    private val userProfile: UserProfileManager
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val activeTasks = mutableMapOf<String, ExecutableTask>()

    private val taskSystemPrompt = """
        You are SAM (Smart Autonomous Machine), an Android AI assistant.
        When the user requests a task, break it into executable steps.
        Respond with JSON: {"steps":[{"id":1,"action":"launch_app","target":"com.instagram.android","description":"Open Instagram"}],"requiresConfirmation":false}
        
        Available actions: launch_app, go_home, go_back, tap_element, long_press, swipe, scroll, type_text, select_element, read_screen, open_notification, adjust_volume, open_camera, open_browser, open_maps, media_control, wait_ui, find_element, submit_search
    """.trimIndent()

    suspend fun planAndExecute(userCommand: String): TaskResult {
        val userName = userProfile.getUserName()
        val taskPlan = modelRouter.planTask(userCommand, taskSystemPrompt)

        if (taskPlan.steps.isEmpty()) {
            return TaskResult(
                taskId = "",
                success = false,
                message = "Sorry $userName, I couldn't plan that task. Please try rephrasing."
            )
        }

        val task = ExecutableTask(
            id = "task_${System.currentTimeMillis()}",
            description = userCommand,
            steps = taskPlan.steps.map { step ->
                TaskStepExecution(
                    stepId = step.id,
                    action = step.action,
                    target = step.target,
                    parameters = step.parameters,
                    description = step.description
                )
            }
        )

        activeTasks[task.id] = task
        return executeTask(task)
    }

    private suspend fun executeTask(task: ExecutableTask): TaskResult {
        task.currentState = TaskState.RUNNING

        for (step in task.steps) {
            step.state = TaskState.RUNNING
            val result = withTimeoutOrNull(30_000L) {
                androidControl.executeAction(step.action, step.target, step.parameters)
            }

            if (result == null) {
                step.state = TaskState.FAILED
                step.error = "Timeout"
                task.currentState = TaskState.FAILED
                return TaskResult(
                    taskId = task.id,
                    success = false,
                    message = "Step '${step.description}' timed out.",
                    failedStep = step.stepId,
                    failureReason = "Operation timed out"
                )
            }

            if (!result.first) {
                step.state = TaskState.FAILED
                step.error = result.second
                task.currentState = TaskState.FAILED
                val userName = userProfile.getUserName()
                return TaskResult(
                    taskId = task.id,
                    success = false,
                    message = "Sorry $userName, I couldn't complete that task. ${result.second}",
                    failedStep = step.stepId,
                    failureReason = result.second
                )
            }

            step.state = TaskState.COMPLETED
            step.result = result.second
            delay(500)
        }

        task.currentState = TaskState.COMPLETED
        val userName = userProfile.getUserName()
        return TaskResult(
            taskId = task.id,
            success = true,
            message = "Done, $userName."
        )
    }

    fun getActiveTasks(): List<ExecutableTask> = activeTasks.values.toList()
    fun getTask(id: String): ExecutableTask? = activeTasks[id]

    fun cancelTask(id: String) {
        activeTasks[id]?.currentState = TaskState.CANCELLED
    }
}
