package com.ferhat.comappav

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.io.File
import java.util.concurrent.Executors

/**
 * The app's own event log, shown in the Console category: commands and their status changes,
 * Firebase errors, connection drops, sign-in problems - and crashes, so a crash's stack trace
 * is still there on the next launch.
 *
 * Kept in memory for the UI (newest last) and in a small file in the app's private storage, so it
 * survives restarts. Only the newest [MAX_ENTRIES] are kept. Every entry also goes to logcat
 * under the tag "ComAppAV". Never log passwords or tokens here: the log is meant to be copied out.
 */
object AppLog {
    const val MAX_ENTRIES = 300
    private const val LOGCAT_TAG = "ComAppAV"
    private const val FILE_NAME = "app-log.txt"
    private const val MAX_MESSAGE_CHARS = 4_000

    /** Oldest first. Observed by Compose; only changed on the main thread. */
    val entries: SnapshotStateList<LogEntry> = mutableStateListOf()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    @Volatile private var file: File? = null
    private var linesOnDisk = 0 // touched only on the io thread

    /**
     * Call from MainActivity.onCreate, before anything logs. Returns true only the first time in
     * this process (a rotation recreates the activity, not the app), so callers can log a start.
     */
    fun init(context: Context): Boolean {
        if (file != null) return false
        val logFile = File(context.applicationContext.filesDir, FILE_NAME)
        file = logFile
        val loaded = try {
            if (logFile.exists()) logFile.readLines().mapNotNull(LogFormat::decode) else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        entries.addAll(loaded.takeLast(MAX_ENTRIES))
        io.execute { linesOnDisk = loaded.size }
        installCrashHandler()
        return true
    }

    fun info(tag: String, message: String) = log(LogLevel.INFO, tag, message)

    fun warn(tag: String, message: String) = log(LogLevel.WARN, tag, message)

    fun error(tag: String, message: String, throwable: Throwable? = null) =
        log(LogLevel.ERROR, tag, if (throwable == null) message else "$message\n${throwable.stackTraceToString()}")

    fun clear() {
        runOnMain { entries.clear() }
        io.execute {
            try {
                file?.writeText("")
                linesOnDisk = 0
            } catch (e: Exception) {
                Log.w(LOGCAT_TAG, "Could not clear the log file", e)
            }
        }
    }

    private fun log(level: LogLevel, tag: String, message: String) {
        val entry = LogEntry(System.currentTimeMillis(), level, tag, message.take(MAX_MESSAGE_CHARS))
        val priority = when (level) {
            LogLevel.INFO -> Log.INFO
            LogLevel.WARN -> Log.WARN
            LogLevel.ERROR -> Log.ERROR
        }
        Log.println(priority, LOGCAT_TAG, "[$tag] ${entry.message}")
        runOnMain {
            entries.add(entry)
            while (entries.size > MAX_ENTRIES) entries.removeAt(0)
        }
        io.execute { appendToDisk(entry) }
    }

    private fun appendToDisk(entry: LogEntry) {
        val target = file ?: return
        try {
            target.appendText(LogFormat.encode(entry) + "\n")
            linesOnDisk++
            // Let the file grow to twice the limit, then cut it back, instead of rewriting it
            // on every single entry.
            if (linesOnDisk > MAX_ENTRIES * 2) {
                val kept = target.readLines().takeLast(MAX_ENTRIES)
                target.writeText(kept.joinToString(separator = "\n", postfix = "\n"))
                linesOnDisk = kept.size
            }
        } catch (e: Exception) {
            Log.w(LOGCAT_TAG, "Could not write the log file", e)
        }
    }

    /**
     * Records an uncaught exception synchronously (the process is about to die, so there is no
     * time to hand it to the io thread), then lets the previous handler crash the app as usual.
     */
    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val entry = LogEntry(
                    System.currentTimeMillis(),
                    LogLevel.ERROR,
                    "Crash",
                    "Uncaught on thread '${thread.name}': ${throwable.stackTraceToString()}".take(MAX_MESSAGE_CHARS),
                )
                file?.appendText(LogFormat.encode(entry) + "\n")
            } catch (ignored: Throwable) {
                // Never let logging get in the way of the real crash report.
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }
}
