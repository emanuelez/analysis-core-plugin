package hudson.plugins.analysis.util;

import static org.junit.Assert.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import hudson.FilePath;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class FilesFinderTest {
    
    @Test
    public void findTest() throws URISyntaxException, IOException, InterruptedException {
        final String resourceFile = "ActionBinding.cs";
        URL resource = FilesFinder.class.getResource(resourceFile);
        File file = new File(resource.toURI());
        File ancestorFolder = file.getParentFile().getParentFile().getParentFile().getParentFile();
        FilePath ancestorFilePath = new FilePath(ancestorFolder);
        Multimap<String,String> result = ancestorFilePath.act(new FilesFinder(Sets.newHashSet("ActionBinding.cs")));
        assertTrue(result.get(resourceFile).size() == 1);
        assertTrue(Lists.newArrayList(result.get(resourceFile)).get(0).endsWith("hudson/plugins/analysis/util/"+resourceFile));
    }
}
