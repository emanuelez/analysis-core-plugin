package hudson.plugins.analysis.util;

import hudson.FilePath;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FileFinderTest {

    @Test
    public void findFileByFilenameTest() throws URISyntaxException, IOException, InterruptedException {
        final String resourceFile = "ActionBinding.cs";
        URL resource = FileFinder.class.getResource(resourceFile);
        File file = new File(resource.toURI());
        File ancestorFolder = file.getParentFile().getParentFile().getParentFile().getParentFile();
        FilePath ancestorFilePath = new FilePath(ancestorFolder);
        String[] result = ancestorFilePath.act(new FileFinder("**/ActionBinding.cs"));
        assertEquals(1, result.length);
        assertTrue(result[0].endsWith("hudson/plugins/analysis/util/" + resourceFile));
    }

}
