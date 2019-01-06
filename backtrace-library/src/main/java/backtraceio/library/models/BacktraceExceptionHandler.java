package backtraceio.library.models;

import backtraceio.library.BacktraceClient;
import backtraceio.library.models.json.BacktraceReport;

public class BacktraceExceptionHandler implements Thread.UncaughtExceptionHandler {
    /**
     *
     */
    private final Thread.UncaughtExceptionHandler rootHandler;
    private BacktraceClient client;

    private BacktraceExceptionHandler(BacktraceClient client) {
        this.client = client;
        rootHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * Enable catching unexpected exceptions by BacktraceClient
     *
     * @param client current Backtrace client instance
     *               which will be used to send information about exception
     */
    public static void enable(BacktraceClient client) {
        new BacktraceExceptionHandler(client);
    }

    /**
     * Called when a thread stops because of an uncaught exception
     *
     * @param thread    thread that is about to exit
     * @param throwable uncaught exception
     */
    @Override
    public void uncaughtException(final Thread thread, final Throwable throwable) {
        if (throwable instanceof Exception) {
            this.client.send(new BacktraceReport((Exception) throwable));
        }
        rootHandler.uncaughtException(thread, throwable);
    }
}