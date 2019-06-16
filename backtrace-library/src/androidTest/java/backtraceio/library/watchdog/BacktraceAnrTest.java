package backtraceio.library.watchdog;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.runner.AndroidJUnit4;

import net.jodah.concurrentunit.Waiter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import backtraceio.library.BacktraceClient;
import backtraceio.library.BacktraceCredentials;
import backtraceio.library.anr.BacktraceANRWatchdog;
import backtraceio.library.anr.BacktraceWatchdogTimeoutException;
import backtraceio.library.anr.OnApplicationNotRespondingEvent;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.logger.LogLevel;

import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class BacktraceAnrTest {
    private Context context;
    private BacktraceCredentials credentials = new BacktraceCredentials("https://example-endpoint.com/", "");
    private BacktraceClient backtraceClient;

    @Before
    public void setUp() {
        this.context = InstrumentationRegistry.getContext();
        this.backtraceClient = new BacktraceClient(this.context, credentials);
    }


    @Test
    @UiThreadTest
    public void checkIfANRIsDetectedCorrectly() {
        // GIVEN
        final Waiter waiter = new Waiter();
        BacktraceLogger.setLevel(LogLevel.DEBUG);
        BacktraceANRWatchdog watchdog = new BacktraceANRWatchdog(this.backtraceClient, 500);
        watchdog.setOnApplicationNotRespondingEvent(new OnApplicationNotRespondingEvent() {
            @Override
            public void onEvent(BacktraceWatchdogTimeoutException exception) {
                waiter.resume();
            }
        });

        // WHEN
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        // THEN
        try {
            waiter.await(5, TimeUnit.SECONDS); // Check if anr is detected and event was emitted
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    @UiThreadTest
    public void checkIfANRIsDetectedCorrectlyWithBacktraceClient() {
        // GIVEN
        final Waiter waiter = new Waiter();
        BacktraceLogger.setLevel(LogLevel.DEBUG);
        this.backtraceClient.enableAnr(500, new OnApplicationNotRespondingEvent() {
            @Override
            public void onEvent(BacktraceWatchdogTimeoutException exception) {
                waiter.resume();
            }
        });

        // WHEN
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        // THEN
        try {
            waiter.await(5, TimeUnit.SECONDS); // Check if anr is detected and event was emitted
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    @UiThreadTest
    public void checkIfANRIsNotDetected() {
        // GIVEN
        final int numberOfIterations = 5;
        final Waiter waiter = new Waiter();
        BacktraceLogger.setLevel(LogLevel.DEBUG);
        BacktraceANRWatchdog watchdog = new BacktraceANRWatchdog(this.backtraceClient, 5000);
        watchdog.setOnApplicationNotRespondingEvent(new OnApplicationNotRespondingEvent() {
            @Override
            public void onEvent(BacktraceWatchdogTimeoutException exception) {
                waiter.fail();
            }
        });

        // WHEN
        try {
            for (int i = 0; i < numberOfIterations; i++) {
                Thread.sleep(800);
                waiter.resume();
            }
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        // THEN
        try {
            waiter.await(5, TimeUnit.SECONDS, numberOfIterations);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    @UiThreadTest
    public void checkIsDisableWorks(){
        final Waiter waiter = new Waiter();
        BacktraceLogger.setLevel(LogLevel.DEBUG);

        backtraceClient.enableAnr(1000, new OnApplicationNotRespondingEvent() {
            @Override
            public void onEvent(BacktraceWatchdogTimeoutException exception) {
                waiter.fail();
            }
        });

        backtraceClient.disableAnr();

        // WHEN
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }
}