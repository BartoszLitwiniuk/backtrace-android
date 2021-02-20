package backtraceio.library.breadcrumbs;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import backtraceio.library.enums.BacktraceBreadcrumbType;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

@RunWith(AndroidJUnit4.class)
public class BacktraceBreadcrumbsTest {
    public Context context;
    EnumSet<BacktraceBreadcrumbType> enabledBreadcrumbTypes = EnumSet.of(BacktraceBreadcrumbType.MANUAL);
    static {
        System.loadLibrary("backtrace-native");
    }

    @Before
    public void setUp() throws IOException {
        this.context = InstrumentationRegistry.getContext();
    }

    @After
    public void cleanUp() {
        File dir = new File(context.getFilesDir().getAbsolutePath() + "/breadcrumbs");
        deleteRecursive(dir);
    }

    @Test
    public void testAddBreadcrumb() {
        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs(enabledBreadcrumbTypes, true, null);

            assertTrue(backtraceBreadcrumbs.addBreadcrumb("Test"));

            List<String> breadcrumbLogFileData = readBreadcrumbLogFile();

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(1));

            assertEquals("Test", parsedBreadcrumb.get("message"));

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testClearBreadcrumbs() {
        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs(enabledBreadcrumbTypes, true, null);

            assertTrue(backtraceBreadcrumbs.addBreadcrumb("Test"));

            List<String> breadcrumbLogFileData = readBreadcrumbLogFile();
            assertEquals(2, breadcrumbLogFileData.size());

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(1));

            assertEquals("Test", parsedBreadcrumb.get("message"));

            assertTrue(backtraceBreadcrumbs.clearBreadcrumbs());

            // Should have cleared the breadcrumb we just read but
            // We should still have a configuration breadcrumb
            breadcrumbLogFileData = readBreadcrumbLogFile();
            assertEquals(1, breadcrumbLogFileData.size());
            parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(0));

            assertEquals("Breadcrumbs configuration", parsedBreadcrumb.get("message"));

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testEnableBreadcrumbs() {
        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs(enabledBreadcrumbTypes, true, null);

            assertTrue(backtraceBreadcrumbs.addBreadcrumb("Test"));

            List<String> breadcrumbLogFileData = readBreadcrumbLogFile();

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(1));

            assertEquals("Test", parsedBreadcrumb.get("message"));

            backtraceBreadcrumbs.disableBreadcrumbs();
            // We get a new breadcrumb because the configuration changed (breadcrumbs disabled)
            breadcrumbLogFileData = readBreadcrumbLogFile();
            assertEquals(3, breadcrumbLogFileData.size());
            parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(2));
            assertEquals("Breadcrumbs configuration", parsedBreadcrumb.get("message"));

            assertFalse(backtraceBreadcrumbs.addBreadcrumb("Test2"));

            // Existing breadcrumbs should be there but no new ones
            breadcrumbLogFileData = readBreadcrumbLogFile();
            assertEquals(3, breadcrumbLogFileData.size());
            parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(2));
            assertEquals("Breadcrumbs configuration", parsedBreadcrumb.get("message"));

            // Now add new ones again
            backtraceBreadcrumbs.enableBreadcrumbs(enabledBreadcrumbTypes, true, null);
            // New configuration breadcrumb because configuration changed (breadcrumbs enabled)
            breadcrumbLogFileData = readBreadcrumbLogFile();
            assertEquals(4, breadcrumbLogFileData.size());
            parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(3));
            assertEquals("Breadcrumbs configuration", parsedBreadcrumb.get("message"));

            assertTrue(backtraceBreadcrumbs.addBreadcrumb("Test2"));

            breadcrumbLogFileData = readBreadcrumbLogFile();
            assertEquals(5, breadcrumbLogFileData.size());
            parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(4));

            assertEquals("Test2", parsedBreadcrumb.get("message"));

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testAddBreadcrumbWithAttributes() {
        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs(enabledBreadcrumbTypes, true, null);

            Map<String, Object> attributes = new HashMap<String, Object>() {{
                put("floopy", "doopy");
                put("flim", "flam");
            }};

            backtraceBreadcrumbs.addBreadcrumb("Test", attributes);

            List<String> breadcrumbLogFileData = readBreadcrumbLogFile();

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(1));

            assertEquals("Test", parsedBreadcrumb.get("message"));
            assertEquals("doopy", parsedBreadcrumb.getJSONObject("attributes").get("floopy"));
            assertEquals("flam", parsedBreadcrumb.getJSONObject("attributes").get("flim"));

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    // We should preserve spaces in the message string
    @Test
    public void testSpaceInMessage() {
        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs(enabledBreadcrumbTypes, true, null);

            backtraceBreadcrumbs.addBreadcrumb("Testing 1 2 3");

            List<String> breadcrumbLogFileData = readBreadcrumbLogFile();

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(1));

            assertEquals("Testing 1 2 3", parsedBreadcrumb.get("message"));

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    // We should remove \n in the message string
    @Test
    public void testNewlineInMessage() {
        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs(enabledBreadcrumbTypes, true, null);

            backtraceBreadcrumbs.addBreadcrumb("Testing\n 1 2\n 3\n");

            List<String> breadcrumbLogFileData = readBreadcrumbLogFile();

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(1));

            assertEquals("Testing 1 2 3", parsedBreadcrumb.get("message"));

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    // We should NOT preserve spaces or newlines for any non-message field
    @Test
    public void testInvalidCharsInAttribute() {
        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs(enabledBreadcrumbTypes, true, null);

            Map<String, Object> attributes = new HashMap<String, Object>() {{
                put(" flo opy", "do o py ");
                put("fl\nim", "fl\nam\n");
                put(" foo ", "b\na r ");
            }};

            backtraceBreadcrumbs.addBreadcrumb("Test", attributes);

            List<String> breadcrumbLogFileData = readBreadcrumbLogFile();

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(1));

            assertEquals("Test", parsedBreadcrumb.get("message"));
            assertEquals("do o py ", parsedBreadcrumb.getJSONObject("attributes").get(" flo opy"));
            assertEquals("flam", parsedBreadcrumb.getJSONObject("attributes").get("flim"));
            assertEquals("ba r ", parsedBreadcrumb.getJSONObject("attributes").get(" foo "));

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testLongMessage() {
        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs(enabledBreadcrumbTypes, true, null);

            backtraceBreadcrumbs.addBreadcrumb(longTestMessage);

            List<String> breadcrumbLogFileData = readBreadcrumbLogFile();

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(1));

            assertEquals(expectedLongTestMessage, parsedBreadcrumb.get("message"));

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testLongAttributesLongFirst() {
        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs(enabledBreadcrumbTypes, true, null);

            final Map<String, Object> attributes = new LinkedHashMap<String, Object>() {{
                put(longTestAttributeKey, longTestAttributeValue);
                put(reasonableLengthAttributeKey, reasonableLengthAttributeValue);
            }};

            backtraceBreadcrumbs.addBreadcrumb("Test", attributes);

            List<String> breadcrumbLogFileData = readBreadcrumbLogFile();

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(1));

            assertEquals("Test", parsedBreadcrumb.get("message"));
            assertEquals(expectedLongTestAttributeValue, parsedBreadcrumb.getJSONObject("attributes").get(expectedLongTestAttributeKey));
            assertFalse(parsedBreadcrumb.getJSONObject("attributes").has(reasonableLengthAttributeKey));

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testLongAttributesShortFirst() {
        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs(enabledBreadcrumbTypes, true, null);

            final Map<String, Object> attributes = new LinkedHashMap<String, Object>() {{
                put(reasonableLengthAttributeKey, reasonableLengthAttributeValue);
                put(longTestAttributeKey, longTestAttributeValue);
            }};

            backtraceBreadcrumbs.addBreadcrumb("Test", attributes);

            List<String> breadcrumbLogFileData = readBreadcrumbLogFile();

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(1));

            assertEquals("Test", parsedBreadcrumb.get("message"));
            assertEquals(reasonableLengthAttributeValue, parsedBreadcrumb.getJSONObject("attributes").get(reasonableLengthAttributeKey));
            assertFalse(parsedBreadcrumb.getJSONObject("attributes").has(expectedLongTestAttributeKey));

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testQueueFileShouldNotRollover() {
        int numIterations = 450;

        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs(enabledBreadcrumbTypes, true, null);

            for (int i = 0; i < numIterations; i++) {
                final long threadId = Thread.currentThread().getId();
                Map<String, Object> attributes = new HashMap<String, Object>() {{
                    put("From Thread", threadId);
                }};
                backtraceBreadcrumbs.addBreadcrumb("I am a breadcrumb", attributes);
            }

            List<String> breadcrumbLogFileData = readBreadcrumbLogFile();

            // First breadcrumb is configuration breadcrumb, it should be valid
            JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(0));
            assertEquals("Breadcrumbs configuration", parsedBreadcrumb.get("message"));
            assertTrue(((int) parsedBreadcrumb.get("id")) == 0);

            // We start from the second breadcrumb
            for (int i = 1; i < breadcrumbLogFileData.size(); i++) {
                parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(i));
                assertEquals("I am a breadcrumb", parsedBreadcrumb.get("message"));
                assertNotNull(parsedBreadcrumb.getJSONObject("attributes").get("From Thread"));
                assertEquals("manual", parsedBreadcrumb.get("type"));
                assertEquals("info", parsedBreadcrumb.get("level"));
                // Timestamp should be convertible to a long
                assertTrue(parsedBreadcrumb.get("timestamp") instanceof Long);
                // Id should be convertible to an int
                assertTrue(parsedBreadcrumb.get("id") instanceof Integer);
            }
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testQueueFileRollover() {
        int numIterations = 1000;

        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs(enabledBreadcrumbTypes, true, null);

            for (int i = 0; i < numIterations; i++) {
                final long threadId = Thread.currentThread().getId();
                Map<String, Object> attributes = new HashMap<String, Object>() {{
                    put("From Thread", threadId);
                }};
                backtraceBreadcrumbs.addBreadcrumb("I am a breadcrumb", attributes);
            }

            List<String> breadcrumbLogFileData = readBreadcrumbLogFile();

            // We should have rolled over the configuration breadcrumb, consider all breadcrumbs here
            for (int i = 0; i < breadcrumbLogFileData.size(); i++) {
                JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(i));
                assertEquals("I am a breadcrumb", parsedBreadcrumb.get("message"));
                assertNotNull(parsedBreadcrumb.getJSONObject("attributes").get("From Thread"));
                assertEquals("manual", parsedBreadcrumb.get("type"));
                assertEquals("info", parsedBreadcrumb.get("level"));
                // Timestamp should be convertible to an int
                assertTrue(parsedBreadcrumb.get("timestamp") instanceof Long);
                // Id should be convertible to an int
                assertTrue(((int)parsedBreadcrumb.get("id")) > 450);
            }
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testQueueFileShouldNotRolloverCustomMax() {
        int numIterations = 45;

        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context, 6400);
            backtraceBreadcrumbs.enableBreadcrumbs(enabledBreadcrumbTypes, true, null);

            for (int i = 0; i < numIterations; i++) {
                final long threadId = Thread.currentThread().getId();
                Map<String, Object> attributes = new HashMap<String, Object>() {{
                    put("From Thread", threadId);
                }};
                backtraceBreadcrumbs.addBreadcrumb("I am a breadcrumb", attributes);
            }

            List<String> breadcrumbLogFileData = readBreadcrumbLogFile();

            // First breadcrumb is configuration breadcrumb, it should be valid
            JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(0));
            assertEquals("Breadcrumbs configuration", parsedBreadcrumb.get("message"));
            assertTrue(((int) parsedBreadcrumb.get("id")) == 0);

            // We start from the second breadcrumb
            for (int i = 1; i < breadcrumbLogFileData.size(); i++) {
                parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(i));
                assertEquals("I am a breadcrumb", parsedBreadcrumb.get("message"));
                assertNotNull(parsedBreadcrumb.getJSONObject("attributes").get("From Thread"));
                assertEquals("manual", parsedBreadcrumb.get("type"));
                assertEquals("info", parsedBreadcrumb.get("level"));
                // Timestamp should be convertible to a long
                assertTrue(parsedBreadcrumb.get("timestamp") instanceof Long);
                // Id should be convertible to an int
                assertTrue(parsedBreadcrumb.get("id") instanceof Integer);
            }
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testQueueFileRolloverCustomMax() {
        int numIterations = 100;

        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context, 6400);
            backtraceBreadcrumbs.enableBreadcrumbs(enabledBreadcrumbTypes, true, null);

            for (int i = 0; i < numIterations; i++) {
                final long threadId = Thread.currentThread().getId();
                Map<String, Object> attributes = new HashMap<String, Object>() {{
                    put("From Thread", threadId);
                }};
                backtraceBreadcrumbs.addBreadcrumb("I am a breadcrumb", attributes);
            }

            List<String> breadcrumbLogFileData = readBreadcrumbLogFile();

            // We should have rolled over the configuration breadcrumb, consider all breadcrumbs here
            for (int i = 0; i < breadcrumbLogFileData.size(); i++) {
                JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(i));
                assertEquals("I am a breadcrumb", parsedBreadcrumb.get("message"));
                assertNotNull(parsedBreadcrumb.getJSONObject("attributes").get("From Thread"));
                assertEquals("manual", parsedBreadcrumb.get("type"));
                assertEquals("info", parsedBreadcrumb.get("level"));
                // Timestamp should be convertible to a long
                assertTrue(parsedBreadcrumb.get("timestamp") instanceof Long);
                // Id should be convertible to an int
                assertTrue(((int)parsedBreadcrumb.get("id")) > 45);
            }
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void breadcrumbsEnduranceTest() {
        int numIterationsPerThread = 200;
        int numThreads = 10;
        Thread[] threads = new Thread[numThreads];

        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs(enabledBreadcrumbTypes, true, null);

            for (int i = 0; i < numThreads; i++) {
                threads[i] = new Thread(new BreadcrumbLogger(backtraceBreadcrumbs, numIterationsPerThread));
                threads[i].start();
            }
            for (int i = 0; i < numThreads; i++) {
                threads[i].join();
            }

            List<String> breadcrumbLogFileData = readBreadcrumbLogFile();

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            for (int i = 1; i < breadcrumbLogFileData.size(); i++) {
                JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(i));
                assertEquals("I am a breadcrumb", parsedBreadcrumb.get("message"));
                assertNotNull(parsedBreadcrumb.getJSONObject("attributes").get("From Thread"));
                assertEquals("manual", parsedBreadcrumb.get("type"));
                assertEquals("info", parsedBreadcrumb.get("level"));
                // Timestamp should be convertible to a long
                assertTrue(parsedBreadcrumb.get("timestamp") instanceof Long);
                // Id should be convertible to an int
                assertTrue(parsedBreadcrumb.get("id") instanceof Integer);
            }
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    public void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }

        fileOrDirectory.delete();
    }

    class BreadcrumbLogger implements Runnable {
        BacktraceBreadcrumbs backtraceBreadcrumbs;
        int numIterations;

        BreadcrumbLogger(BacktraceBreadcrumbs backtraceBreadcrumbs, int numIterations) {
            this.backtraceBreadcrumbs = backtraceBreadcrumbs;
            this.numIterations = numIterations;
        }

        @Override
        public void run() {
            for (int i = 0; i < this.numIterations; i++) {
                final long threadId = Thread.currentThread().getId();
                Map<String, Object> attributes = new HashMap<String, Object>() {{
                    put("From Thread", threadId);
                }};

                backtraceBreadcrumbs.addBreadcrumb("I am a breadcrumb", attributes);
            }
        }
    }

    public List<String> readBreadcrumbLogFile() throws IOException {
        File breadcrumbLogFile = new File(BacktraceBreadcrumbsLogger.getBreadcrumbLogPath(context));

        List<String> breadcrumbLogFileData = new ArrayList<String>();
        FileInputStream inputStream = new FileInputStream(breadcrumbLogFile.getAbsolutePath());

        // The encoding contains headers for the encoded data
        // We just throw away lines that don't start with "timestamp
        StringBuilder stringBuilder = new StringBuilder();
        while (inputStream.available() > 0) {
            char c = (char) inputStream.read();
            if (c == '\n') {
                String line = stringBuilder.toString();
                if (line.matches(".*timestamp.*")) {
                    breadcrumbLogFileData.add(line);
                }
                stringBuilder = new StringBuilder();
                continue;
            }
            stringBuilder.append(c);
        }

        return breadcrumbLogFileData;
    }

    private final String longTestMessage = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ipsum consequat nisl vel pretium lectus quam id. Velit dignissim sodales ut eu sem integer vitae justo. Cursus euismod quis viverra nibh cras pulvinar. Pellentesque adipiscing commodo elit at imperdiet. Pellentesque eu tincidunt tortor aliquam nulla facilisi cras fermentum. Elementum facilisis leo vel fringilla est ullamcorper eget nulla. Purus sit amet luctus venenatis. Non consectetur a erat nam at. Pellentesque id nibh tortor id aliquet lectus proin. Purus semper eget duis at tellus. Sodales ut etiam sit amet nisl purus. Viverra justo nec ultrices dui sapien eget.\n" +
            "\n" +
            "Et ultrices neque ornare aenean euismod elementum nisi quis eleifend. Ut diam quam nulla porttitor. Vitae elementum curabitur vitae nunc sed. Feugiat sed lectus vestibulum mattis ullamcorper velit sed. A diam sollicitudin tempor id eu nisl nunc. At urna condimentum mattis pellentesque id. Arcu odio ut sem nulla pharetra diam sit amet. Ipsum dolor sit amet consectetur adipiscing elit duis. Adipiscing vitae proin sagittis nisl rhoncus mattis rhoncus. Faucibus interdum posuere lorem ipsum dolor. Aliquet risus feugiat in ante metus dictum at. Pretium aenean pharetra magna ac placerat vestibulum lectus mauris ultrices. Enim nulla aliquet porttitor lacus luctus accumsan. Diam ut venenatis tellus in metus. Facilisi nullam vehicula ipsum a arcu cursus.\n" +
            "\n" +
            "Sed faucibus turpis in eu mi bibendum neque egestas congue. Ipsum nunc aliquet bibendum enim facilisis gravida neque convallis. Vitae congue mauris rhoncus aenean vel elit scelerisque mauris pellentesque. Id donec ultrices tincidunt arcu non sodales neque. Eu turpis egestas pretium aenean pharetra magna ac. Est ullamcorper eget nulla facilisi etiam dignissim diam. Eget arcu dictum varius duis at. Pretium quam vulputate dignissim suspendisse in est. Morbi quis commodo odio aenean sed adipiscing diam. Leo urna molestie at elementum eu.";

    private final String expectedLongTestMessage = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ipsum consequat nisl vel pretium lectus quam id. Velit dignissim sodales ut eu sem integer vitae justo. Cursus euismod quis viverra nibh cras pulvinar. Pellentesque adipiscing commodo elit at imperdiet. Pellentesque eu tincidunt tortor aliquam nulla facilisi cras fermentum. Elementum facilisis leo vel fringilla est ullamcorper eget nulla. Purus sit amet luctus venenatis. Non consectetur a erat nam at. Pellentesque id nibh tortor id aliquet lectus proin. Purus semper eget duis at tellus. Sodales ut etiam sit amet nisl purus. Viverra justo nec ultrices dui sapien eget.Et ultrices neque ornare aenean euismod elementum nisi quis eleifend. Ut diam quam nulla porttitor. Vitae elementum curabitur vitae nunc sed. Feugiat sed lectus vestibulum mattis ullamcorper velit sed. A diam sollicitudin tempor id eu nisl nunc. At urna condimentum mattis pellentesque id. Arcu odio ut sem nulla pharetra dia";

    private final String longTestAttributeKey = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ipsum consequat nisl vel pretium lectus quam id. Velit dignissim sodales ut eu sem integer vitae justo. Cursus euismod quis viverra nibh cras pulvinar. Pellentesque adipiscing commodo elit at imperdiet. Pellentesque eu tincidunt tortor aliquam nulla facilisi cras fermentum. Elementum facilisis leo vel fringilla est ullamcorper eget nulla. Purus sit amet luctus venenatis. Non consectetur a erat nam at. Pellentesque id nibh tortor id aliquet lectus proin. Purus semper eget duis at tellus. Sodales ut etiam sit amet nisl purus. Viverra justo nec ultrices dui sapien eget.";

    private final String longTestAttributeValue = "Et ultrices neque ornare aenean euismod elementum nisi quis eleifend. Ut diam quam nulla porttitor. Vitae elementum curabitur vitae nunc sed. Feugiat sed lectus vestibulum mattis ullamcorper velit sed. A diam sollicitudin tempor id eu nisl nunc. At urna condimentum mattis pellentesque id. Arcu odio";

    private final String expectedLongTestAttributeKey = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ipsum consequat nisl vel pretium lectus quam id. Velit dignissim sodales ut eu sem integer vitae justo. Cursus euismod quis viverra nibh cras pulvinar. Pellentesque adipiscing commodo elit at imperdiet. Pellentesque eu tincidunt tortor aliquam nulla facilisi cras fermentum. Elementum facilisis leo vel fringilla est ullamcorper eget nulla. Purus sit amet luctus venenatis. Non consectetur a erat nam at. Pellentesque id nibh tortor id aliquet lectus proin. Purus semper eget duis at tellus. Sodales ut etiam sit amet nisl purus. Viverra justo nec ultrices dui sapien eget.";

    private final String expectedLongTestAttributeValue = "Et ultrices neque ornare aenean euismod elementum nisi quis eleifend. Ut diam quam nulla porttitor. Vitae elementum curabitur vitae nunc sed. Feugiat sed lectus vestibulum mattis ullamcorper velit sed. A diam sollicitudin tempor id eu nisl nunc. At urna condimentum mattis pellentesque id. Arcu odio";

    private final String reasonableLengthAttributeKey = "reasonablySizedKey";

    private final String reasonableLengthAttributeValue = "reasonableSizedValue";
}
