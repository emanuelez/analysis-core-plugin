package hudson.plugins.analysis.util;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
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

    private Multimap<String, String> results = HashMultimap.create();

    private BiMap<String, List<String>> filesToFind = HashBiMap.create();

    private BiMap<String, String> cleanFiles = HashBiMap.create();

    /**
     * Creates a new instance of {@link FilesFinder}.
     *
     * @param files the file names to look for
     */
    public FilesFinder(Set<String> files) {
        // Split the relative filename and remove everything that comes before a . or .. (included)
        for (String file : files) {
            List<String> splittedFile = Lists.newArrayList(Splitter.on("/").omitEmptyStrings().split(file));
            int deleteUpTo = Math.max(splittedFile.indexOf("."), splittedFile.indexOf(".."));
            if (deleteUpTo >= 0) {
                for (int i = 0; i <= deleteUpTo; i++) {
                    splittedFile.remove(0);
                }
            }
            filesToFind.put(file, splittedFile);
            cleanFiles.put(file, Joiner.on("/").join(splittedFile));
        }
    }

    /**
     * Returns a Multimap with the file paths associated to the file names specified in the constructor
     *
     * @param file           root directory of the workspace
     * @param virtualChannel not used
     * @return the file paths of all found files
     * @throws IOException          if the workspace could not be read
     * @throws InterruptedException if the user sops the build
     */
    public Multimap<String, String> invoke(File file, VirtualChannel virtualChannel) throws IOException, InterruptedException {
        this.walk(file);

        for (String fileToFind : Sets.difference(filesToFind.keySet(), results.keySet())) {
            results.put(fileToFind, null);
        }

        return results;
    }

    private void walk(File dir) {
        File[] listFile = dir.listFiles();
        if (listFile != null) {
            for (final File aListFile : listFile) {
                if (aListFile.isDirectory()) {
                    walk(aListFile);
                } else {
                    boolean isInteresting = Iterables.any(cleanFiles.values(),
                            new Predicate<String>() {
                                public boolean apply(@Nullable String s) {
                                    return aListFile.getPath().endsWith(s);
                                }
                            });
                    if (isInteresting) {
                        String cleanPath = Iterables.find(cleanFiles.values(),
                                new Predicate<String>() {
                                    public boolean apply(@Nullable String s) {
                                        return aListFile.getPath().endsWith(s);
                                    }
                                });
                        results.put(cleanFiles.inverse().get(cleanPath), aListFile.getPath());
                    }
                }
            }
        }
    }

}
