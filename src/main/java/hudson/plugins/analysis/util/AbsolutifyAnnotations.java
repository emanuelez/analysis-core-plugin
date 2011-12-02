package hudson.plugins.analysis.util;

import com.google.common.base.Predicate;
import com.google.common.collect.*;
import hudson.FilePath;
import hudson.plugins.analysis.util.model.FileAnnotation;
import hudson.remoting.VirtualChannel;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;

public class AbsolutifyAnnotations implements FilePath.FileCallable<Iterable<? extends FileAnnotation>> {

    private Iterable<? extends FileAnnotation> annotations;

    private Multimap<String, FileAnnotation> relative = HashMultimap.create();

    private BiMap<String, String> relativeToAbsolute = HashBiMap.create();

    public AbsolutifyAnnotations(Iterable<? extends FileAnnotation> annotations) {
        this.annotations = annotations;
    }

    public Iterable<? extends FileAnnotation> invoke(File file, VirtualChannel virtualChannel) throws IOException, InterruptedException {

        Iterable<? extends FileAnnotation> relativeAnnotations = Iterables.filter(
                annotations,
                new Predicate<FileAnnotation>() {
                    public boolean apply(@Nullable FileAnnotation fileAnnotation) {
                        return fileAnnotation != null && !fileAnnotation.isPathAbsolute();
                    }
                });


        for (FileAnnotation relativeAnnotation : relativeAnnotations) {
            relative.put(relativeAnnotation.getPathName(), relativeAnnotation);
        }

        walk(file);

        for (FileAnnotation annotation : annotations) {
            if (!annotation.isPathAbsolute()) {
                annotation.setFileName(relativeToAbsolute.get(annotation.getPathName()));
            }
        }

        return annotations;
    }

    private void walk(File dir) {
        File[] listFile = dir.listFiles();
        if (listFile != null) {
            for (final File aListFile : listFile) {
                if (aListFile.isDirectory()) {
                    walk(aListFile);
                } else {
                    boolean isInteresting = Iterables.any(
                            relative.keys(),
                            new Predicate<String>() {
                                public boolean apply(@Nullable String s) {
                                    return aListFile.getPath().endsWith(s);
                                }
                            });
                    if (isInteresting) {
                        String cleanPath = Iterables.find(
                                relative.keys(),
                                new Predicate<String>() {
                                    public boolean apply(@Nullable String s) {
                                        return aListFile.getPath().endsWith(s);
                                    }
                                });
                        relativeToAbsolute.put(cleanPath, aListFile.getPath());
                    }
                }
            }
        }
    }

}
