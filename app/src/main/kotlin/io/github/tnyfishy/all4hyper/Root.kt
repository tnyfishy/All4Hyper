package io.github.tnyfishy.all4hyper

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Root through `su -c`. Calls block, so they run on the IO dispatcher. */
object Root {
    private const val TIMEOUT_SECONDS = 10L

    data class Result(val ok: Boolean, val output: String, val error: String)

    /** True when su answers with uid 0; the first call is what shows the manager's grant prompt. */
    suspend fun available(): Boolean = run("id").let { it.ok && "uid=0" in it.output }

    /** Stops every process of the package so the hooks and the new settings load on its next start. */
    suspend fun restart(packageName: String): Boolean = run("am force-stop $packageName").ok

    suspend fun run(script: String): Result = withContext(Dispatchers.IO) {
        var process: Process? = null
        try {
            process = ProcessBuilder("su", "-c", script).start()
            // su waits on stdin otherwise.
            runCatching { process.outputStream.close() }
            val output = StringBuilder()
            val error = StringBuilder()
            val readers = listOf(
                Thread { process.inputStream.bufferedReader().useLines { lines -> lines.forEach { output.appendLine(it) } } },
                Thread { process.errorStream.bufferedReader().useLines { lines -> lines.forEach { error.appendLine(it) } } },
            ).onEach { it.isDaemon = true; it.start() }
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroy()
                return@withContext Result(false, output.toString(), "timed out")
            }
            readers.forEach { it.join(1500) }
            Result(process.exitValue() == 0, output.toString(), error.toString())
        } catch (failure: Throwable) {
            process?.destroy()
            Result(false, "", failure.message ?: failure.javaClass.simpleName)
        }
    }
}
