package com.meneses.budgethunter.commons.data.sync

/**
 * Logging abstraction for sync operations.
 * Provides structured logging with different severity levels.
 */
interface Logger {
    fun debug(tag: String, message: String)
    fun info(tag: String, message: String)
    fun warn(tag: String, message: String, error: Throwable? = null)
    fun error(tag: String, message: String, error: Throwable? = null)
}

/**
 * Console logger implementation using println.
 * For production, this should be replaced with a proper logging framework.
 */
class ConsoleLogger : Logger {
    override fun debug(tag: String, message: String) {
        println("DEBUG [$tag] $message")
    }

    override fun info(tag: String, message: String) {
        println("INFO [$tag] $message")
    }

    override fun warn(tag: String, message: String, error: Throwable?) {
        println("WARN [$tag] $message")
        error?.let { println("  Caused by: ${it.message}") }
    }

    override fun error(tag: String, message: String, error: Throwable?) {
        println("ERROR [$tag] $message")
        error?.let {
            println("  Caused by: ${it.message}")
            it.printStackTrace()
        }
    }
}

/**
 * No-op logger for tests or when logging is disabled.
 */
class NoOpLogger : Logger {
    override fun debug(tag: String, message: String) {}
    override fun info(tag: String, message: String) {}
    override fun warn(tag: String, message: String, error: Throwable?) {}
    override fun error(tag: String, message: String, error: Throwable?) {}
}
