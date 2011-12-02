package hudson.plugins.analysis.util;

import com.google.common.collect.Sets;
import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;
import org.springframework.util.AntPathMatcher;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Scans the workspace and finds all files matching a give pattern.
 *
 * @author Ulli Hafner
 */
public class FileFinder implements FileCallable<String[]> {
    /** Generated ID. */
    private static final long serialVersionUID = 2970029366847565970L;
    /** The pattern to scan for. */
    private final String pattern;
    /** The results */
    private Set<String> results = Sets.newHashSet();

    /**
     * Creates a new instance of {@link FileFinder}.
     *
     * @param pattern the ant file pattern to scan for
     */
    public FileFinder(final String pattern) {
        this.pattern = pattern;
    }

    /**
     * Returns an array with the filenames of the specified file pattern that
     * have been found in the workspace.
     *
     * @param workspace
     *            root directory of the workspace
     * @param channel
     *            not used
     * @return the filenames of all found files
     * @throws IOException
     *             if the workspace could not be read
     */
    public String[] invoke(final File workspace, final VirtualChannel channel) throws IOException {
        results.clear();
        walk(workspace);
        return results.toArray(new String[results.size()]);
    }
    
    public String[] find(File dir) {
        results.clear();
        walk(dir);
        return results.toArray(new String[results.size()]);
    }
    
    private String removeRoot(String path) {
        int index = 0;
        if (path.startsWith("/")) index = 1;
        if (path.matches("^[a-zA-Z]:/.+")) index = 3;
        return path.substring(index);
    }

    private void walk(File dir) {
        File[] listFile = dir.listFiles();
        if (listFile != null) {
            for (final File aListFile : listFile) {
                if (aListFile.isDirectory()) {
                    walk(aListFile);
                } else {
                    AntPathMatcher matcher = new AntPathMatcher();
                    if (matcher.match(pattern, this.removeRoot(aListFile.getPath()))) {
                        results.add(aListFile.getPath());
                    }
                }
            }
        }
    }
}