package hudson.plugins.analysis.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.Sets;
import hudson.FilePath;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AbsolutifyAnnotationsTest {

    @Test
    public void findSingleFileTest() throws Exception {
        final String resourceFile = "ActionBinding.cs";
        URL resource = FileFinder.class.getResource(resourceFile);
        File file = new File(resource.toURI());
        File ancestorFolder = file.getParentFile().getParentFile().getParentFile().getParentFile();
        FilePath ancestorFilePath = new FilePath(ancestorFolder);
        Iterable<String> relativePaths = Sets.newHashSet("../ActionBinding.cs");

        BiMap<String, String> result = ancestorFilePath.act(new AbsolutifyAnnotations(relativePaths));
        assertEquals(1, result.size());
        assertTrue(result.get("../ActionBinding.cs").endsWith("hudson/plugins/analysis/util/" + resourceFile));
    }

    @Test
    public void findTwoFilesTest() throws Exception {
        final String resourceFile = "ActionBinding.cs";
        URL resource = FileFinder.class.getResource(resourceFile);
        File file = new File(resource.toURI());
        File ancestorFolder = file.getParentFile().getParentFile().getParentFile().getParentFile();
        FilePath ancestorFilePath = new FilePath(ancestorFolder);
        Iterable<String> relativePaths = Sets.newHashSet("../ActionBinding.cs", "util/ActionBinding-Original-Formatting.cs");

        BiMap<String, String> result = ancestorFilePath.act(new AbsolutifyAnnotations(relativePaths));
        assertEquals(2, result.size());
        assertTrue(result.get("../ActionBinding.cs").endsWith("hudson/plugins/analysis/util/" + resourceFile));
        assertTrue(result.get("util/ActionBinding-Original-Formatting.cs").endsWith("hudson/plugins/analysis/util/ActionBinding-Original-Formatting.cs"));
        assertFalse(result.get("../ActionBinding.cs").startsWith("NOT FOUND"));
        assertFalse(result.get("util/ActionBinding-Original-Formatting.cs").startsWith("NOT FOUND"));
    }

    @Test
    public void findTwoFilesTestWithLimit() throws Exception {
        final String resourceFile = "ActionBinding.cs";
        URL resource = FileFinder.class.getResource(resourceFile);
        File file = new File(resource.toURI());
        File ancestorFolder = file.getParentFile().getParentFile().getParentFile().getParentFile();
        FilePath ancestorFilePath = new FilePath(ancestorFolder);
        Iterable<String> relativePaths = Sets.newHashSet("../ActionBinding.cs", "util/ActionBinding-Original-Formatting.cs");

        BiMap<String, String> result = ancestorFilePath.act(new AbsolutifyAnnotations(relativePaths, 2));
        assertEquals(2, result.size());
        assertTrue(result.get("../ActionBinding.cs").startsWith("NOT FOUND"));
        assertTrue(result.get("util/ActionBinding-Original-Formatting.cs").startsWith("NOT FOUND"));
    }

}
