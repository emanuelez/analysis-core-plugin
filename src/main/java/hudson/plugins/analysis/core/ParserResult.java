package hudson.plugins.analysis.core;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import hudson.FilePath;
import hudson.plugins.analysis.Messages;
import hudson.plugins.analysis.util.AbsolutifyAnnotations;
import hudson.plugins.analysis.util.FileFinder;
import hudson.plugins.analysis.util.model.FileAnnotation;
import hudson.plugins.analysis.util.model.Priority;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * Stores the collection of parsed annotations and associated error messages.
 * This class is not thread safe.
 *
 * @author Ulli Hafner
 */
public class ParserResult implements Serializable {
    private static final long serialVersionUID = -8414545334379193330L;

    /** The parsed annotations. */
    @SuppressWarnings("Se")
    private final Set<FileAnnotation> annotations = Sets.newHashSet();
    /** The collection of error messages. */
    @SuppressWarnings("Se")
    private final List<String> errorMessages = Lists.newArrayList();
    /** Number of annotations by priority. */
    @SuppressWarnings("Se")
    private final Map<Priority, Integer> annotationCountByPriority = Maps.newHashMap();
    /** The set of modules. */
    @SuppressWarnings("Se")
    private final Set<String> modules = Sets.newHashSet();
    /** The workspace. */
    private final Workspace workspace;
    /** The log messages. @since 1.20 **/
    private String logMessage;
    /** Total number of modules. @since 1.31 **/
    private int numberOfModules;

    /**
     * Facade for the remote workspace.
     */
    interface Workspace extends Serializable {
        Workspace child(String fileName);

        String getPath();

        String[] findFiles(String pattern) throws IOException, InterruptedException;
        
        BiMap<String, String> absolutifyAnnotations(Iterable<String> annotations) throws IOException, InterruptedException;
    }

    /**
     * Creates a new instance of {@link ParserResult}.
     */
    public ParserResult() {
        this(new NullWorkspace());
    }

    /**
     * Creates a new instance of {@link ParserResult}.
     *
     * @param workspace
     *            the workspace to find the files in
     */
    public ParserResult(final FilePath workspace) {
        this(new FilePathAdapter(workspace));
    }

    /**
     * Creates a new instance of {@link ParserResult}.
     *
     * @param workspace
     *            the workspace to find the files in
     */
    public ParserResult(final Workspace workspace) {
        this.workspace = workspace;

        Priority[] priorities = Priority.values();

        for (Priority priority1 : priorities) {
            annotationCountByPriority.put(priority1, 0);
        }
    }

    /**
     * Creates a new instance of {@link ParserResult}.
     *
     * @param annotations
     *            the annotations to add
     */
    public ParserResult(final Collection<? extends FileAnnotation> annotations) {
        this(new NullWorkspace());

        addAnnotations(annotations);
    }

    /**
     * Adds the warnings of the specified project to this project.
     *
     * @param additionalProject the project to add
     */
    public void addProject(final ParserResult additionalProject) {
        addAnnotations(additionalProject.getAnnotations());
        addErrors(additionalProject.getErrorMessages());
        addModules(additionalProject.getModules());
    }

    /**
     * Adds the specified annotation to this container.
     *
     * @param annotation the annotation to add
     */
    public final void addAnnotation(final FileAnnotation annotation) {
        if (!annotations.contains(annotation)) {
            annotations.add(annotation);
            Integer count = annotationCountByPriority.get(annotation.getPriority());
            annotationCountByPriority.put(annotation.getPriority(), count + 1);
        }
    }

    /**
     * Adds the specified annotations to this container.
     *
     * @param newAnnotations the annotations to add
     */
    public final void addAnnotations(final Collection<? extends FileAnnotation> newAnnotations) {
        
        // Find the annotations with relative paths
        Iterable<? extends FileAnnotation> relativeAnnotations = Iterables.filter(newAnnotations, new Predicate<FileAnnotation>() {
            public boolean apply(@Nullable FileAnnotation annotation) {
                return annotation != null && !annotation.isPathAbsolute();
            }
        });

        // Get the relative paths
        Iterable<String> relativePaths = Iterables.transform(relativeAnnotations, new Function<FileAnnotation, String>() {
            public String apply(@Nullable FileAnnotation fileAnnotation) {
                if (fileAnnotation != null) {
                    return fileAnnotation.getFileName();
                } else {
                    return null;
                }
            }
        });

        try {
            BiMap<String, String> relativeToAbsolute = workspace.absolutifyAnnotations(relativePaths);

            for (FileAnnotation annotation : newAnnotations) {
                if (Iterables.contains(relativeAnnotations, annotation)) {
                    annotation.setFileName(relativeToAbsolute.get(annotation.getFileName()));
                }
                addAnnotation(annotation);
            }
        } catch (IOException e) {
            // ignore
        } catch (InterruptedException e) {
            // ignore
        }

    }

    /**
     * Adds the specified annotations to this container.
     *
     * @param newAnnotations the annotations to add
     */
    public final void addAnnotations(final FileAnnotation[] newAnnotations) {
        addAnnotations(Arrays.asList(newAnnotations));
    }

    /**
     * Adds an error message for the specified module name.
     *
     * @param module
     *            the current module
     * @param message
     *            the error message
     */
    public void addErrorMessage(final String module, final String message) {
        errorMessages.add(Messages.Result_Error_ModuleErrorMessage(module, message));
    }

    /**
     * Adds an error message.
     *
     * @param message
     *            the error message
     */
    public void addErrorMessage(final String message) {
        errorMessages.add(message);
    }

    /**
     * Adds the error messages to this result.
     *
     * @param errors the error messages to add
     */
    public void addErrors(final List<String> errors) {
        errorMessages.addAll(errors);
    }

    /**
     * Returns the errorMessages.
     *
     * @return the errorMessages
     */
    public List<String> getErrorMessages() {
        return ImmutableList.copyOf(errorMessages);
    }

    /**
     * Returns the annotations of this result.
     *
     * @return the annotations of this result
     */
    public Set<FileAnnotation> getAnnotations() {
        return ImmutableSet.copyOf(annotations);
    }

    /**
     * Returns the total number of annotations for this object.
     *
     * @return total number of annotations for this object
     */
    public int getNumberOfAnnotations() {
        return annotations.size();
    }

    /**
     * Returns the total number of annotations of the specified priority for
     * this object.
     *
     * @param priority
     *            the priority
     * @return total number of annotations of the specified priority for this
     *         object
     */
    public int getNumberOfAnnotations(final Priority priority) {
        return annotationCountByPriority.get(priority);
    }

    /**
     * Returns whether this objects has annotations.
     *
     * @return <code>true</code> if this objects has annotations.
     */
    public boolean hasAnnotations() {
        return !annotations.isEmpty();
    }

    /**
     * Returns whether this objects has annotations with the specified priority.
     *
     * @param priority
     *            the priority
     * @return <code>true</code> if this objects has annotations.
     */
    public boolean hasAnnotations(final Priority priority) {
        return annotationCountByPriority.get(priority) > 0;
    }

    /**
     * Returns whether this objects has no annotations.
     *
     * @return <code>true</code> if this objects has no annotations.
     */
    public boolean hasNoAnnotations() {
        return !hasAnnotations();
    }

    /**
     * Returns whether this objects has no annotations with the specified priority.
     *
     * @param priority
     *            the priority
     * @return <code>true</code> if this objects has no annotations.
     */
    public boolean hasNoAnnotations(final Priority priority) {
        return !hasAnnotations(priority);
    }

    /**
     * Returns the number of modules.
     *
     * @return the number of modules
     */
    public int getNumberOfModules() {
        return numberOfModules;
    }

    /**
     * Returns the parsed modules.
     *
     * @return the parsed modules
     */
    public Set<String> getModules() {
        return Collections.unmodifiableSet(modules);
    }

    /**
     * Adds a new parsed module.
     *
     * @param moduleName
     *            the name of the parsed module
     */
    public void addModule(final String moduleName) {
        modules.add(moduleName);

        numberOfModules++;
    }

    /**
     * Adds the specified parsed modules.
     *
     * @param additionalModules
     *            the name of the parsed modules
     */
    public void addModules(final Collection<String> additionalModules) {
        modules.addAll(additionalModules);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getNumberOfAnnotations() + " annotations";
    }

    /**
     * Sets the log messages of the parsing process.
     *
     * @param message a multiline message
     * @since 1.20
     */
    public void setLog(final String message) {
        logMessage = message;
    }

    /**
     * Returns the log messages of the parsing process.
     *
     * @return the messages
     * @since 1.20
     */
    public String getLogMessages() {
        return StringUtils.defaultString(logMessage);
    }

    /**
     * Default implementation that delegates to an {@link FilePath} instance.
     */
    private static class FilePathAdapter implements Workspace {
        private static final long serialVersionUID = 1976601889843466249L;

        private final FilePath wrapped;

        /**
         * Creates a new instance of {@link FilePathAdapter}.
         *
         * @param workspace
         *            the {@link FilePath} to wrap
         */
        FilePathAdapter(final FilePath workspace) {
            wrapped = workspace;
        }

        /** {@inheritDoc} */
        public Workspace child(final String fileName) {
            return new FilePathAdapter(wrapped.child(fileName));
        }

        /** {@inheritDoc} */
        public boolean exists() throws IOException, InterruptedException {
            return wrapped.exists();
        }

        /** {@inheritDoc} */
        public String getPath() {
            return wrapped.getRemote();
        }

        /** {@inheritDoc} */
        public String[] findFiles(final String pattern) throws IOException, InterruptedException {
            return wrapped.act(new FileFinder(pattern));
        }

        public BiMap<String, String> absolutifyAnnotations(Iterable<String> annotations) throws IOException, InterruptedException {
            return wrapped.act(new AbsolutifyAnnotations(annotations));
        }
    }

    /**
     * Null pattern.
     */
    private static class NullWorkspace implements Workspace {
        private static final long serialVersionUID = 2307259492760554066L;

        /** {@inheritDoc} */
        public Workspace child(final String fileName) {
            return this;
        }

        /** {@inheritDoc} */
        public boolean exists() throws IOException, InterruptedException {
            return false;
        }

        /** {@inheritDoc} */
        public String getPath() {
            return StringUtils.EMPTY;
        }

        /** {@inheritDoc} */
        public String[] findFiles(final String pattern) throws IOException, InterruptedException {
            return new String[0];
        }

        /** {@inheritDoc} */
        public BiMap<String, String> absolutifyAnnotations(Iterable<String> annotations) throws IOException, InterruptedException {
            return HashBiMap.create();
        }
    }

}

