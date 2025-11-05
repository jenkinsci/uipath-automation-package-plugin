package com.uipath.uipathpackage.solutions;

import com.google.common.collect.ImmutableList;
import com.uipath.uipathpackage.entries.SelectEntry;
import com.uipath.uipathpackage.entries.authentication.ExternalAppAuthenticationEntry;
import com.uipath.uipathpackage.entries.authentication.TokenAuthenticationEntry;
import com.uipath.uipathpackage.entries.authentication.UserPassAuthenticationEntry;
import com.uipath.uipathpackage.models.solutions.SolutionPackOptions;
import com.uipath.uipathpackage.models.solutions.SolutionAnalyzeOptions;
import com.uipath.uipathpackage.util.*;
import hudson.*;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.*;

import static hudson.slaves.WorkspaceList.tempDir;

/**
 * Performs the actual solution packaging build.
 */
public class UiPathSolutionPack extends Builder implements SimpleBuildStep {
    private final Utility util = new Utility();
    private final String version;
    private final String workspacePath;
    private final String outputPath;
    private Boolean disableBuiltInNugetFeeds;
    private boolean runWorkflowAnalysis;
    private String repositoryUrl;
    private String repositoryCommit;
    private String repositoryBranch;
    private String repositoryType;
    private String projectUrl;
    private String releaseNotes;
    private boolean useOrchestrator;
    private String orchestratorAddress;
    private String orchestratorTenant;
    private SelectEntry credentials;
    private final TraceLevel traceLevel;
    private String governanceFilePath;

    /**
     * Data bound constructor responsible for setting the values param values to state
     *
     * @param version       Version string (mandatory)
     * @param workspacePath Workspace Path
     * @param outputPath    Output Path
     * @param traceLevel    The trace logging level. One of the following values: None, Critical, Error, Warning, Information, Verbose. (default None)
     */
    @DataBoundConstructor
    public UiPathSolutionPack(String version, String workspacePath, String outputPath, TraceLevel traceLevel) {
        this.version = version;
        this.workspacePath = workspacePath;
        this.outputPath = outputPath;
        this.traceLevel = traceLevel;
        this.disableBuiltInNugetFeeds = null;
        this.repositoryUrl = null;
        this.repositoryCommit = null;
        this.repositoryBranch = null;
        this.repositoryType = null;
        this.projectUrl = null;
        this.releaseNotes = null;

        this.orchestratorAddress = "";
        this.orchestratorTenant = "";
        this.credentials = null;
        this.runWorkflowAnalysis = false;
        this.governanceFilePath = null;
    }

    /**
     * Run this step.
     *
     * @param run       a build this is running as a part of
     * @param workspace a workspace to use for any file operations
     * @param launcher  a way to start processes
     * @param listener  a place to send output
     * @throws InterruptedException if the step is interrupted
     * @throws IOException          if something goes wrong
     */
    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull EnvVars env, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        validateParameters();

        FilePath tempRemoteDir = tempDir(workspace);
        /**
         * Adding the null check here as above method "tempDir" is annotated with @CheckForNull
         * and findbugs plugin will report an error of NPE while building the plugin.
         */
        if (Objects.isNull(tempRemoteDir)) {
            throw new AbortException(com.uipath.uipathpackage.Messages.GenericErrors_FailedToCreateTempFolderPack());
        }
        tempRemoteDir.mkdirs();

        try {
            EnvVars envVars = TaskScopedEnvVarsManager.addRequiredEnvironmentVariables(run, env, listener);
            util.validateRuntime(launcher, envVars);

            CliDetails cliDetails = util.getCliDetails(run, listener, envVars, launcher);
            String buildTag = envVars.get(EnvironmentVariablesConsts.BUILD_TAG);

            FilePath expandedOutputPath = outputPath.contains("${WORKSPACE}") ?
                    new FilePath(launcher.getChannel(), envVars.expand(outputPath)) :
                    workspace.child(envVars.expand(outputPath));
            expandedOutputPath.mkdirs();

            FilePath expandedWorkspacePath = workspacePath.contains("${WORKSPACE}") ?
                    new FilePath(launcher.getChannel(), envVars.expand(workspacePath)) :
                    workspace.child(envVars.expand(workspacePath));

            if (runWorkflowAnalysis) {
                SolutionAnalyzeOptions solutionAnalyzeOptions = new SolutionAnalyzeOptions();
                if (governanceFilePath != null && !governanceFilePath.isEmpty()) {
                    FilePath expandedGovernanceFilePath = governanceFilePath.contains("${WORKSPACE}") ?
                            new FilePath(launcher.getChannel(), envVars.expand(governanceFilePath)) :
                            workspace.child(envVars.expand(governanceFilePath));
                    solutionAnalyzeOptions.setGovernanceFilePath(expandedGovernanceFilePath.getRemote());
                }
                if (disableBuiltInNugetFeeds != null && disableBuiltInNugetFeeds) {
                    solutionAnalyzeOptions.setDisableBuiltInNugetFeeds(true);
                }

                if (cliDetails.getActualVersion().supportsNewTelemetry()) {
                    solutionAnalyzeOptions.populateAdditionalTelemetryData();
                    solutionAnalyzeOptions.setPipelineCorrelationId(buildTag);
                    solutionAnalyzeOptions.setCliGetFlow(cliDetails.getGetFlow());
                }
                solutionAnalyzeOptions.setProjectPath(expandedWorkspacePath.getRemote());

                if (useOrchestrator) {
                    solutionAnalyzeOptions.setOrchestratorUrl(orchestratorAddress);
                    solutionAnalyzeOptions.setOrchestratorTenant(orchestratorTenant);

                    util.setCredentialsFromCredentialsEntry(credentials, solutionAnalyzeOptions, run);
                }

                util.execute("SolutionAnalyzeOptions", solutionAnalyzeOptions, tempRemoteDir, listener, envVars, launcher, true);
            }

            SolutionPackOptions solutionPackOptions = new SolutionPackOptions();
            if (cliDetails.getActualVersion().supportsNewTelemetry()) {
                solutionPackOptions.populateAdditionalTelemetryData();
                solutionPackOptions.setPipelineCorrelationId(buildTag);
                solutionPackOptions.setCliGetFlow(cliDetails.getGetFlow());
            }

            solutionPackOptions.setDestinationFolder(expandedOutputPath.getRemote());
            solutionPackOptions.setProjectPath(expandedWorkspacePath.getRemote());

            if (disableBuiltInNugetFeeds != null && disableBuiltInNugetFeeds) {
                solutionPackOptions.setDisableBuiltInNugetFeeds(true);
            }

            solutionPackOptions.setRepositoryUrl(repositoryUrl);
            solutionPackOptions.setRepositoryCommit(repositoryCommit);
            solutionPackOptions.setRepositoryBranch(repositoryBranch);
            solutionPackOptions.setRepositoryType(repositoryType);
            solutionPackOptions.setProjectUrl(projectUrl);
            solutionPackOptions.setReleaseNotes(releaseNotes);

            // Version is now a simple string, expanded from environment variables
            solutionPackOptions.setVersion(envVars.expand(version.trim()));

            if (useOrchestrator) {
                solutionPackOptions.setOrchestratorUrl(orchestratorAddress);
                solutionPackOptions.setOrchestratorTenant(orchestratorTenant);

                util.setCredentialsFromCredentialsEntry(credentials, solutionPackOptions, run);
            }

            String language = Locale.getDefault().getLanguage();
            String country = Locale.getDefault().getCountry();
            String localization = country.isEmpty() ? language : language + "-" + country;
            solutionPackOptions.setLanguage(localization);

            solutionPackOptions.setTraceLevel(traceLevel);

            util.execute("SolutionPackOptions", solutionPackOptions, tempRemoteDir, listener, envVars, launcher, true);
        } catch (URISyntaxException e) {
            e.printStackTrace(listener.getLogger());
            throw new AbortException(e.getMessage());
        } finally {
            try {
                Objects.requireNonNull(tempRemoteDir).deleteRecursive();
            } catch (Exception e) {
                listener.getLogger().println(com.uipath.uipathpackage.Messages.GenericErrors_FailedToDeleteTempPack() + e.getMessage());
            }
        }
    }

    @DataBoundSetter
    public void setUseOrchestrator(boolean useOrchestrator) {
        this.useOrchestrator = useOrchestrator;

        if (!useOrchestrator) {
            this.orchestratorAddress = null;
            this.orchestratorTenant = null;
            this.credentials = null;
        }
    }

    @DataBoundSetter
    public void setGovernanceFilePath(String governanceFilePath) {
        this.governanceFilePath = governanceFilePath;
    }

    @DataBoundSetter
    public void setDisableBuiltInNugetFeeds(Boolean disableBuiltInNugetFeeds) {
        this.disableBuiltInNugetFeeds = disableBuiltInNugetFeeds;
    }

    @DataBoundSetter
    public void setRunWorkflowAnalysis(boolean runWorkflowAnalysis) {
        this.runWorkflowAnalysis = runWorkflowAnalysis;
    }

    @DataBoundSetter
    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    @DataBoundSetter
    public void setRepositoryCommit(String repositoryCommit) {
        this.repositoryCommit = repositoryCommit;
    }

    @DataBoundSetter
    public void setRepositoryBranch(String repositoryBranch) {
        this.repositoryBranch = repositoryBranch;
    }

    @DataBoundSetter
    public void setRepositoryType(String repositoryType) {
        this.repositoryType = repositoryType;
    }

    @DataBoundSetter
    public void setProjectUrl(String projectUrl) {
        this.projectUrl = projectUrl;
    }

    @DataBoundSetter
    public void setReleaseNotes(String releaseNotes) {
        this.releaseNotes = releaseNotes;
    }

    @DataBoundSetter
    public void setOrchestratorAddress(String orchestratorAddress) {
        this.orchestratorAddress = orchestratorAddress;
    }

    @DataBoundSetter
    public void setOrchestratorTenant(String orchestratorTenant) {
        this.orchestratorTenant = orchestratorTenant;
    }

    @DataBoundSetter
    public void setCredentials(SelectEntry credentials) {
        this.credentials = credentials;
    }

    /**
     * Provide the version
     *
     * @return String version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Provides the workspace path
     *
     * @return String workspacePath
     */
    public String getWorkspacePath() {
        return workspacePath;
    }

    /**
     * Use orchestrator
     *
     * @return boolean useOrchestrator
     */
    public boolean getUseOrchestrator() {
        return useOrchestrator;
    }

    /**
     * Orchestrator address
     *
     * @return String orchestratorAddress
     */
    public String getOrchestratorAddress() {
        return orchestratorAddress;
    }

    /**
     * Orchestrator tenant
     *
     * @return String orchestratorTenant
     */
    public String getOrchestratorTenant() {
        return orchestratorTenant;
    }

    /**
     * Credentials
     *
     * @return SelectEntry credentials
     */
    public SelectEntry getCredentials() {
        return credentials;
    }

    /**
     * Provides the Output Path
     *
     * @return String outputPath
     */
    public String getOutputPath() {
        return outputPath;
    }

    public Boolean getDisableBuiltInNugetFeeds() {
        return disableBuiltInNugetFeeds;
    }

    /**
     * Provides the run workflow analysis flag
     *
     * @return boolean runWorkflowAnalysis
     */
    public boolean getRunWorkflowAnalysis() {
        return runWorkflowAnalysis;
    }

    public String getGovernanceFilePath() {
        return governanceFilePath;
    }

    /**
     * Provides the repository url
     *
     * @return String repositoryUrl
     */
    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    /**
     * Provides the repository commit
     *
     * @return String repositoryCommit
     */
    public String getRepositoryCommit() {
        return repositoryCommit;
    }

    /**
     * Provides the repository branch
     *
     * @return String repositoryBranch
     */
    public String getRepositoryBranch() {
        return repositoryBranch;
    }

    /**
     * Provides the repository type
     *
     * @return String repositoryType
     */
    public String getRepositoryType() {
        return repositoryType;
    }

    /**
     * Provides the project url
     *
     * @return String projectUrl
     */
    public String getProjectUrl() {
        return projectUrl;
    }

    public String getReleaseNotes() {
        return releaseNotes;
    }

    /**
     * traceLevel
     *
     * @return TraceLevel traceLevel
     */
    public TraceLevel getTraceLevel() {
        return traceLevel;
    }

    private void validateParameters() throws AbortException {
        if (version == null || version.trim().isEmpty()) {
            throw new InvalidParameterException(com.uipath.uipathpackage.Messages.GenericErrors_MissingVersioningMethod());
        }

        util.validateParams(workspacePath, com.uipath.uipathpackage.Messages.ValidationErrors_InvalidProject());
        util.validateParams(outputPath, com.uipath.uipathpackage.Messages.ValidationErrors_InvalidOutputPath());

        if (useOrchestrator) {
            util.validateParams(orchestratorAddress, com.uipath.uipathpackage.Messages.ValidationErrors_InvalidOrchAddress());

            if (credentials == null) {
                throw new InvalidParameterException(com.uipath.uipathpackage.Messages.ValidationErrors_InvalidCredentialsType());
            }

            credentials.validateParameters();
        }

        if (outputPath.toUpperCase().contains("${JENKINS_HOME}")) {
            throw new AbortException(com.uipath.uipathpackage.Messages.ValidationErrors_InvalidPath());
        }
    }

    /**
     * {@link Descriptor} for {@link Builder}
     */
    @Symbol("UiPathSolutionPack")
    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        /**
         * Returns true if this task is applicable to the given project.
         *
         * @return true to allow user to configure this post-promotion task for the given project.
         * @see AbstractProject.AbstractProjectDescriptor#isApplicable(Descriptor)
         */
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        /**
         * Validates the version
         *
         * @param value Version value
         * @return FormValidation
         */
        public FormValidation doCheckVersion(@QueryParameter String value) {
            if (value.trim().isEmpty()) {
                return FormValidation.error(com.uipath.uipathpackage.Messages.GenericErrors_MissingVersioningMethod());
            }
            return FormValidation.ok();
        }

        /**
         * Validated the Workspace path
         *
         * @param value Workspace Path value
         * @return FormValidation
         */
        public FormValidation doCheckWorkspacePath(@QueryParameter String value) {
            if (value.trim().isEmpty()) {
                return FormValidation.error(com.uipath.uipathpackage.Messages.UiPathSolutionPack_DescriptorImpl_Error_MissingWorkspacePath());
            }

            if (value.trim().toUpperCase().contains("${JENKINS_HOME}")) {
                return FormValidation.error(com.uipath.uipathpackage.Messages.GenericErrors_MustUseSlavePaths());
            }

            return FormValidation.ok();
        }

        /**
         * Validates the output path
         *
         * @param value Output Path value
         * @return FormValidation
         */
        public FormValidation doCheckOutputPath(@QueryParameter String value) {
            if (value.trim().isEmpty()) {
                return FormValidation.error(com.uipath.uipathpackage.Messages.UiPathPack_DescriptorImpl_Error_MissingOutputPath());
            }
            return FormValidation.ok();
        }

        /**
         * Provides the display name to the build step
         *
         * @return String display name
         */
        @Nonnull
        @Override
        public String getDisplayName() {
            return "UiPath Solution: Pack";
        }

        /**
         * Returns the list of Strings to be filled in choice
         * If item is null or doesn't have configure permission it will return empty list
         *
         * @param item Basic configuration unit in Hudson
         * @return ListBoxModel list of String
         */
        public ListBoxModel doFillTraceLevelItems(@AncestorInPath Item item) {
            if (item == null || !item.hasPermission(Item.CONFIGURE)) {
                return new ListBoxModel();
            }

            ListBoxModel result = new ListBoxModel();
            for (TraceLevel v : TraceLevel.values()) {
                result.add(v.toString(), v.toString());
            }

            return result;
        }

        /**
         * Provides the list of descriptors to the choice in hetero-radio
         *
         * @return list of the authentication descriptors
         */
        public List<Descriptor> getAuthenticationDescriptors() {
            Jenkins jenkins = Jenkins.getInstanceOrNull();
            if (jenkins == null) {
                return new ArrayList<>();
            }

            List<Descriptor> list = new ArrayList<>();
            Descriptor userPassDescriptor = jenkins.getDescriptor(UserPassAuthenticationEntry.class);
            if (userPassDescriptor != null) {
                list.add(userPassDescriptor);
            }
            Descriptor tokenDescriptor = jenkins.getDescriptor(TokenAuthenticationEntry.class);
            if (tokenDescriptor != null) {
                list.add(tokenDescriptor);
            }
            Descriptor externalAppDescriptor = jenkins.getDescriptor(ExternalAppAuthenticationEntry.class);
            if (externalAppDescriptor != null) {
                list.add(externalAppDescriptor);
            }
            return ImmutableList.copyOf(list);
        }
    }
}
