package hudson.plugins.analysis.core;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;

import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.google.common.collect.Lists;

import hudson.model.Action;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

import hudson.plugins.analysis.graph.BuildResultGraph;
import hudson.plugins.analysis.graph.DefaultGraphConfigurationView;
import hudson.plugins.analysis.graph.DifferenceGraph;
import hudson.plugins.analysis.graph.EmptyGraph;
import hudson.plugins.analysis.graph.GraphConfiguration;
import hudson.plugins.analysis.graph.HealthGraph;
import hudson.plugins.analysis.graph.NewVersusFixedGraph;
import hudson.plugins.analysis.graph.NullGraph;
import hudson.plugins.analysis.graph.PriorityGraph;
import hudson.plugins.analysis.graph.TotalsGraph;
import hudson.plugins.analysis.graph.UserGraphConfigurationView;
import hudson.plugins.analysis.graph.GraphConfigurationView;
import hudson.plugins.analysis.graph.TrendDetails;

import hudson.util.Graph;

/**
 * A project action displays a link on the side panel of a project. This action
 * also is responsible to render the historical trend via its associated
 * 'floatingBox.jelly' view.
 *
 * @param <T>
 *            result action type
 * @author Ulli Hafner
 */
public abstract class AbstractProjectAction<T extends ResultAction<?>> implements Action  { // NOCHECKSTYLE
    private static final Logger LOGGER = Logger.getLogger(AbstractProjectAction.class.getName());

    /** Project that owns this action. */
    private final AbstractProject<?, ?> project;
    /** The type of the result action.  */
    private final Class<? extends T> resultActionType;
    /** The icon URL of this action: it will be shown as soon as a result is available. */
    private final String iconUrl;
    /** Plug-in URL. */
    private final String url;
    /** Plug-in results URL. */
    private final String resultUrl;

    /**
     * Creates a new instance of <code>AbstractProjectAction</code>.
     *
     * @param project
     *            the project that owns this action
     * @param resultActionType
     *            the type of the result action
     * @param plugin
     *            the plug-in that owns this action
     */
    public AbstractProjectAction(final AbstractProject<?, ?> project, final Class<? extends T> resultActionType, final PluginDescriptor plugin) {
        this.project = project;
        this.resultActionType = resultActionType;
        iconUrl = plugin.getIconUrl();
        url = plugin.getPluginName();
        resultUrl = plugin.getPluginResultUrlName();
    }

    /**
     * Returns the title of the trend graph.
     *
     * @return the title of the trend graph.
     */
    public abstract String getTrendName();

    /**
     * Returns the project.
     *
     * @return the project
     */
    public final AbstractProject<?, ?> getProject() {
        return project;
    }

    /**
     * Returns the graph configuration for this project.
     *
     * @param link
     *            not used
     * @param request
     *            Stapler request
     * @param response
     *            Stapler response
     * @return the dynamic result of the analysis (detail page).
     */
    public Object getDynamic(final String link, final StaplerRequest request, final StaplerResponse response) {
        if ("configureDefaults".equals(link)) {
            return createDefaultConfiguration();
        }
        else if ("configure".equals(link)) {
            return createUserConfiguration(request);
        }
        else {
            return null;
        }
    }

    /**
     * Returns the trend graph details.
     *
     * @return the details
     */
    public Object getTrendDetails() {
        return getTrendDetails(Stapler.getCurrentRequest(), Stapler.getCurrentResponse());
    }

    /**
     * Returns the trend graph details.
     *
     * @param request
     *            Stapler request
     * @param response
     *            Stapler response
     * @return the details
     */
    public Object getTrendDetails(final StaplerRequest request, final StaplerResponse response) {
        return new TrendDetails(getProject(), getTrendGraph(request, response));
    }

    /**
     * Returns the trend graph.
     *
     * @return the current trend graph
     */
    public Object getTrendGraph() {
        return getTrendGraph(Stapler.getCurrentRequest(), Stapler.getCurrentResponse());
    }

    /**
     * Returns the configured trend graph.
     *
     * @param request
     *            Stapler request
     * @param response
     *            Stapler response
     * @return the trend graph
     */
    public Graph getTrendGraph(final StaplerRequest request, final StaplerResponse response) {
        GraphConfigurationView configuration = createUserConfiguration(request);
        if (configuration.hasMeaningfulGraph()) {
            return configuration.getGraphRenderer();
        }
        else {
            BuildResultGraph graphType = configuration.getGraphType();
            try {
                response.sendRedirect2(request.getContextPath() + graphType.getExampleImage());
            }
            catch (IOException exception) {
                LOGGER.log(Level.SEVERE, "Can't create graph: " + graphType, exception);
            }

            return null;
        }
    }

    /**
     * Returns whether the trend graph is visible.
     *
     * @param request
     *            the request to get the cookie from
     * @return <code>true</code> if the trend is visible
     */
    public boolean isTrendVisible(final StaplerRequest request) {
        GraphConfigurationView configuration = createUserConfiguration(request);

        return configuration.isVisible() && configuration.hasMeaningfulGraph();
    }

    /**
     * Returns whether the trend graph is deactivated.
     *
     * @param request
     *            the request to get the cookie from
     * @return <code>true</code> if the trend is deactivated
     */
    public boolean isTrendDeactivated(final StaplerRequest request) {
        return createUserConfiguration(request).isDeactivated();
    }

    /**
     * Returns whether the enable trend graph link should be shown.
     *
     * @param request
     *            the request to get the cookie from
     * @return the graph configuration
     */
    public boolean canShowEnableTrendLink(final StaplerRequest request) {
        GraphConfigurationView configuration = createUserConfiguration(request);
        if (configuration.hasMeaningfulGraph()) {
            return !configuration.isDeactivated() && !configuration.isVisible();
        }
        return false;
    }

    /**
     * Creates a view to configure the trend graph for the current user.
     *
     * @param request
     *            Stapler request
     * @return a view to configure the trend graph for the current user
     */
    protected GraphConfigurationView createUserConfiguration(final StaplerRequest request) {
        return new UserGraphConfigurationView(createConfiguration(), getProject(),
                getUrlName(), request.getCookies(), createBuildHistory());
    }

    /**
     * Creates a view to configure the trend graph defaults.
     *
     * @return a view to configure the trend graph defaults
     */
    protected GraphConfigurationView createDefaultConfiguration() {
        return new DefaultGraphConfigurationView(createConfiguration(), getProject(), getUrlName(), createBuildHistory());
    }

    /**
     * Creates the build history.
     *
     * @return build history
     */
    protected BuildHistory createBuildHistory() {
        AbstractBuild<?, ?> lastFinishedBuild = getLastFinishedBuild();
        if (lastFinishedBuild == null) {
            return new NullBuildHistory();
        }
        else {
            return new BuildHistory(lastFinishedBuild, resultActionType);
        }
    }

    /**
     * Creates the graph configuration.
     *
     * @return the graph configuration
     */
    private GraphConfiguration createConfiguration() {
        return createConfiguration(getAvailableGraphs());
    }

    /**
     * Returns the sorted list of available graphs.
     *
     * @return the available graphs
     */
    protected List<BuildResultGraph> getAvailableGraphs() {
        List<BuildResultGraph> availableGraphs = Lists.newArrayList();

        availableGraphs.add(new NewVersusFixedGraph());
        availableGraphs.add(new PriorityGraph());
        availableGraphs.add(new TotalsGraph());
        if (hasValidResults()) {
            availableGraphs.add(new HealthGraph(getLastAction().getHealthDescriptor()));
        }
        else {
            availableGraphs.add(new HealthGraph(new NullHealthDescriptor()));
        }
        availableGraphs.add(new DifferenceGraph());
        availableGraphs.add(new EmptyGraph());
        availableGraphs.add(new NullGraph());

        return availableGraphs;
    }

    /**
     * Creates the graph configuration.
     *
     * @param availableGraphs
     *            the available graphs
     * @return the graph configuration.
     */
    protected GraphConfiguration createConfiguration(final List<BuildResultGraph> availableGraphs) {
        return new GraphConfiguration(availableGraphs);
    }

    /**
     * Returns whether this project has a valid result action attached.
     *
     * @return <code>true</code> if the results are valid
     */
    public final boolean hasValidResults() {
        return getLastAction() != null;
    }

    /**
     * Returns the icon URL for the side-panel in the project screen. If there
     * is no valid result yet, then <code>null</code> is returned.
     *
     * @return the icon URL for the side-panel in the project screen
     */
    public String getIconFileName() {
        ResultAction<?> lastAction = getLastAction();
        if (lastAction != null && lastAction.getResult().hasAnnotations()) {
            return iconUrl;
        }
        return null;
    }

    /** {@inheritDoc} */
    public final String getUrlName() {
        return url;
    }

    /**
     * Returns the last valid result action.
     *
     * @return the last valid result action, or <code>null</code> if no such action is found
     */
    @CheckForNull
    public ResultAction<?> getLastAction() {
        AbstractBuild<?, ?> lastBuild = getLastFinishedBuild();
        if (lastBuild != null) {
            return lastBuild.getAction(resultActionType);
        }
        return null;
    }

    /**
     * Returns the last finished build.
     *
     * @return the last finished build or <code>null</code> if there is no
     *         such build
     */
    @CheckForNull
    public AbstractBuild<?, ?> getLastFinishedBuild() {
        AbstractBuild<?, ?> lastBuild = project.getLastBuild();
        while (lastBuild != null && (lastBuild.isBuilding() || lastBuild.getAction(resultActionType) == null)) {
            lastBuild = lastBuild.getPreviousBuild();
        }
        return lastBuild;
    }

    /**
     *
     * Redirects the index page to the last result.
     *
     * @param request
     *            Stapler request
     * @param response
     *            Stapler response
     * @throws IOException
     *             in case of an error
     */
    public void doIndex(final StaplerRequest request, final StaplerResponse response) throws IOException {
        AbstractBuild<?, ?> build = getLastFinishedBuild();
        if (build != null) {
            response.sendRedirect2(String.format("../%d/%s", build.getNumber(), resultUrl));
        }
    }
}
