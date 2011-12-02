package hudson.plugins.analysis.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PathStringCleanerTest {
    @Test
    public void testCleanRemoveDots() throws Exception {
        String path = "./some/./path/./test.txt";
        String expected = "some/path/test.txt";

        String result = PathStringCleaner.clean(path);

        assertEquals(expected, result);
    }

    @Test
    public void testCleanRemoveDoubleDots() throws Exception {
        String path = "../../some/../path/./test.txt";
        String expected = "../../path/test.txt";

        String result = PathStringCleaner.clean(path);

        assertEquals(expected, result);
    }
}
