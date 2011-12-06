package hudson.plugins.analysis.util;

import com.google.common.base.Predicate;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import hudson.FilePath;
import hudson.remoting.VirtualChannel;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class AbsolutifyAnnotations implements FilePath.FileCallable<BiMap<String, String>> {

    private List<String> relativePathsLeft;

    private BiMap<String, String> relativeToAbsolute = HashBiMap.create();
    
    private final int maxDepth;

    public AbsolutifyAnnotations(Iterable<String> relativePaths) {
        this.relativePathsLeft = Lists.newArrayList(relativePaths);
        this.maxDepth = 20;
    }

    public AbsolutifyAnnotations(Iterable<String> relativePaths, int maxDepth) {
        this.relativePathsLeft = Lists.newArrayList(relativePaths);
        this.maxDepth = maxDepth;
    }

    public BiMap<String, String> invoke(File file, VirtualChannel virtualChannel) throws IOException, InterruptedException {

        walk(file, 0);

        for (String relativePath : relativePathsLeft) {
            relativeToAbsolute.put(relativePath, "NOT FOUND - " + relativePath);
        }

        return relativeToAbsolute;
    }
    


    private void walk(File dir, int depth) {
        File[] listFile = dir.listFiles();
        if (listFile != null) {
            for (File aListFile : listFile) {
                if (aListFile.isDirectory()) {
                    if (depth < maxDepth) {
                        walk(aListFile, depth + 1);
                    }
                } else {
                    int index = Iterables.indexOf(relativePathsLeft, new FindFilePredicate(aListFile));

                    if (index >= 0) {
                        relativeToAbsolute.put(relativePathsLeft.get(index), aListFile.getAbsolutePath());
                        relativePathsLeft.remove(index);
                    }
                }
            }
        }
    }

    private class FindFilePredicate implements Predicate<String> {
        
        private File currentFile;

        private FindFilePredicate(File currentFile) {
            this.currentFile = currentFile;
        }

        private String removeInitialDoubleDots(String relativePath) {
            String cleanRelativePath = relativePath;
            while (cleanRelativePath.startsWith("../")) {
                cleanRelativePath = cleanRelativePath.substring(3);
            }
            return cleanRelativePath;
        }

        public boolean apply(@Nullable String relativePath) {
            return relativePath != null && currentFile.getAbsolutePath().endsWith(this.removeInitialDoubleDots(relativePath));
        }
    }

}
