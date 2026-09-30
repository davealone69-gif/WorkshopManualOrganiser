package com.workshop.manualorganiser.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.delay

/**
 * Best-effort Android -> Termux bridge for local Workshop services.
 *
 * Android cannot directly start arbitrary Termux processes. When Termux is
 * installed and grants RUN_COMMAND, this manager asks Termux to start Ollama.
 * The app then verifies the real HTTP endpoint before considering the service
 * ready. No fake online state is returned.
 */
object LocalServerManager {
    private const val TERMUX_PACKAGE = "com.termux"
    private const val RUN_COMMAND_ACTION = "com.termux.RUN_COMMAND"
    private const val RUN_COMMAND_PERMISSION = "com.termux.permission.RUN_COMMAND"
    private const val RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"

    private const val OLLAMA_PATH = "/data/data/com.termux/files/usr/bin/ollama"
    private const val OLLAMA_HOST = "127.0.0.1:11434"

    suspend fun ensureOllama(context: Context): Result<String> {
        val initial = AiClient.checkServer()
        if (initial.isSuccess) return initial

        val launch = launchTermuxCommand(
            context = context,
            command = OLLAMA_PATH,
            args = listOf("serve"),
        )
        if (launch.isFailure) {
            return Result.failure(
                launch.exceptionOrNull()
                    ?: IllegalStateException("Termux refused to start Ollama")
            )
        }

        var lastError = initial.exceptionOrNull()?.message ?: "Ollama is offline"
        repeat(15) {
            delay(1000)
            val check = AiClient.checkServer()
            if (check.isSuccess) return check
            lastError = check.exceptionOrNull()?.message ?: lastError
        }

        return Result.failure(
            IllegalStateException(
                "Ollama did not become ready after startup request: $lastError"
            )
        )
    }

    private fun launchTermuxCommand(
        context: Context,
        command: String,
        args: List<String>,
    ): Result<Unit> = runCatching {
        val intent = Intent(RUN_COMMAND_ACTION).apply {
            component = ComponentName(TERMUX_PACKAGE, RUN_COMMAND_SERVICE)
            putExtra("com.termux.RUN_COMMAND_PATH", command)
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", args.toTypedArray())
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
        }
        context.startService(intent)
    }
}
