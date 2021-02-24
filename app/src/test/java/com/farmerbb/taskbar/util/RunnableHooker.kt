package com.farmerbb.taskbar.util

/**
 * [RunnableHooker] for test.
 *
 *
 * It provides method to check whether the [Runnable.run] method has been invoked.
 *
 *
 * Also, the powermock can't process the code likes:
 *
 * <pre>
 * Runnable runnable = new Runnable() {
 * @Override
 * public void run() {}
 * }
 * </pre>
 *
 *
 * or
 *
 * <pre>
 * Runnable runnable = () -> {}
 * </pre>
 *
 *
 * So we create a implementation to avoid powermock exception.
 */
class RunnableHooker : Runnable {
    private var hasRun = false
    override fun run() {
        hasRun = true
    }

    fun hasRun(): Boolean {
        return hasRun
    }
}
