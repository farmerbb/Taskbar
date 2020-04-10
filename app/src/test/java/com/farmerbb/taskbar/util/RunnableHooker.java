package com.farmerbb.taskbar.util;

/**
 * {@link RunnableHooker} for test.
 * <p>
 * It provides method to check whether the {@link Runnable#run} method has been invoked.
 * <p>
 * Also, the powermock can't process the code likes:
 *
 * <pre>
 *     Runnable runnable = new Runnable() {
 *         @Override
 *         public void run() {}
 *     }
 * </pre>
 * <p>
 * or
 *
 * <pre>
 *     Runnable runnable = () -> {}
 * </pre>
 * <p>
 * So we create a implementation to avoid powermock exception.
 */
public class RunnableHooker implements Runnable {
    private boolean hasRun = false;

    @Override
    public void run() {
        hasRun = true;
    }

    public boolean hasRun() {
        return hasRun;
    }
}
