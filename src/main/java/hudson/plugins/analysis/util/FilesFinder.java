package hudson.plugins.analysis.util;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.*;
import hudson.FilePath;
import hudson.remoting.VirtualChannel;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;


/**
 * Finds all the paths possibly associated to a given Set<String> of file names.
 *
 * @author Emanuele Zattin
 */
public class FilesFinder implements FilePath.FileCallable<Multimap<String, String>> {

    private Set<List<String>> filesToFind;

    private Multimap<String, String> results = HashMultimap.create();

    /**
     * Creates a new instance of {@link FilesFinder}.
     *
     * @param filesToFind the file names to look for
     */
    public FilesFinder(Set<String> filesToFind) {
        Set<List<String>> result = Sets.newHashSet();

        // Split the relative filename and remove everything that comes before a . or .. (included)
        for (String file : filesToFind) {
            List<String> splittedFiles = Lists.newArrayList(Splitter.on("/").omitEmptyStrings().split(file));
            int deleteUpTo = Math.max(splittedFiles.indexOf("."), splittedFiles.indexOf(".."));
            if (deleteUpTo >= 0) {
                for (int i = 0; i <= deleteUpTo; i++) {
                    splittedFiles.remove(0);
                }
            }
            result.add(splittedFiles);
        }
        this.filesToFind = result;
    }

    /**
     * Returns a Multimap with the file paths associated to the file names specified in the constructor
     *
     * @param file
     *            root directory of the workspace
     * @param virtualChannel
     *            not used
     * @return the file paths of all found files
     * @throws IOException
     *             if the workspace could not be read
     * @throws InterruptedException
     *             if the user sops the build
     */
    public Multimap<String, String> invoke(File file, VirtualChannel virtualChannel) throws IOException, InterruptedException {
        this.walk(file);

        for (String fileToFind : Sets.difference(filesToFind, results.keySet())) {
            results.put(fileToFind, null);
        }

        return results;
    }

    private void walk(File dir) {
        File[] listFile = dir.listFiles();
        if (listFile != null) {
            for (File aListFile : listFile) {
                if (aListFile.isDirectory()) {
                    walk(aListFile);
                } else {
                    if (filesToFind.contains(aListFile.getName())) {
                        results.put(aListFile.getName(), aListFile.getPath());
                    }
                }
            }
        }
    }

}
