package hudson.plugins.analysis.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import hudson.FilePath;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import java.util.Set;


/**
 * Finds all the paths possibly associated to a given Set<String> of file names.
 *
 * @author Emanuele Zattin
 */
public class FilesFinder implements FilePath.FileCallable<Multimap<String, String>> {

    private Set<String> filesToFind;

    private Multimap<String, String> results = HashMultimap.create();

    /**
     * Creates a new instance of {@link FilesFinder}.
     *
     * @param filesToFind the file names to look for
     */
    public FilesFinder(Set<String> filesToFind) {
        this.filesToFind = filesToFind;
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
