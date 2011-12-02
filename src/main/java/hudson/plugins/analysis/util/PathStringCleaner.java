package hudson.plugins.analysis.util;

import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.List;

public class PathStringCleaner {

    // Suppress default constructor for non-instantiability
    private PathStringCleaner() {
        throw new AssertionError();
    }
    
    public static String clean(String pathString) {
        List<String> splitted = Lists.newArrayList(Splitter.on("/").split(pathString));
        Iterables.removeIf(splitted, Predicates.equalTo("."));
        
        // pop out the pre-pending double dots
        int prepended = 0;
        while ("..".equals(splitted.get(0))) {
            splitted.remove(0);
            prepended++;
        }
        
        int index = Iterables.indexOf(splitted, Predicates.equalTo(".."));
        while (index >= 1) {
            splitted.remove(index - 1);
            splitted.remove(index - 1);
        index = Iterables.indexOf(splitted, Predicates.equalTo(".."));
        }

        for(int i=0; i < prepended; i++) {
            splitted.add(0, "..");
        }
        
        return Joiner.on("/").join(splitted);
    }
}
