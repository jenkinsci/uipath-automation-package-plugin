package com.uipath.uipathpackage;

import com.google.common.collect.ImmutableList;
import com.uipath.uipathpackage.entries.SelectEntry;
import com.uipath.uipathpackage.entries.authentication.ExternalAppAuthenticationEntry;
import com.uipath.uipathpackage.entries.authentication.TokenAuthenticationEntry;
import com.uipath.uipathpackage.entries.authentication.UserPassAuthenticationEntry;
import com.uipath.uipathpackage.entries.versioning.AutoVersionEntry;
import com.uipath.uipathpackage.entries.versioning.CurrentVersionEntry;
import com.uipath.uipathpackage.entries.versioning.ManualVersionEntry;
import com.uipath.uipathpackage.models.AnalyzeOptions;
import com.uipath.uipathpackage.models.PackOptions;
import com.uipath.uipathpackage.util.OutputType;
import com.uipath.uipathpackage.util.TraceLevel;
import com.uipath.uipathpackage.util.Utility;
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
 * Performs the actual build.
 */
public class UiPathPack extends Builder implements SimpleBuildStep {
    private final Utility util = new Utility();
    private final SelectEntry version;
    private final String projectJsonPath;
    private final String outputPath;
    private String outputType;
    private boolean runWorkflowAnalysis;

    private boolean useOrchestrator;
    private String orchestratorAddress;
    private String orchestratorTenant;
    private SelectEntry credentials;
    private final TraceLevel traceLevel;

    /**
     * Data bound constructor responsible for setting the values param values to state
     *
     * @param version         Entry version
     * @param projectJsonPath Project Json Path
     * @param outputPath      Output Path
     * @param traceLevel      The trace logging level. One of the following values: None, Critical, Error, Warning, Information, Verbose. (default None)
     */
    @DataBoundConstructor
    public UiPathPack(SelectEntry version, String projectJsonPath, String outputPath, TraceLevel traceLevel) {
        this.version = version;
        this.projectJsonPath = projectJsonPath;
        this.outputPath = outputPath;
        this.traceLevel = traceLevel;
        this.outputType = "None";

        this.orchestratorAddress = "";
        this.orchestratorTenant = "";
        this.credentials = null;
        this.runWorkflowAnalysis = false;
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
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        validateParameters();

        if (launcher.isUnix()) {
            throw new AbortException(com.uipath.uipathpackage.Messages.GenericErrors_MustUseWindows());
        }

        FilePath tempRemoteDir = tempDir(workspace);
        tempRemoteDir.mkdirs();

        try {
            EnvVars envVars = run.getEnvironment(listener);

            FilePath expandedOutputPath = outputPath.contains("${WORKSPACE}") ?
                    new FilePath(launcher.getChannel(), envVars.expand(outputPath)) :
                    workspace.child(envVars.expand(outputPath));
            expandedOutputPath.mkdirs();

            FilePath expandedProjectJsonPath = projectJsonPath.contains("${WORKSPACE}") ?
                    new FilePath(launcher.getChannel(), envVars.expand(projectJsonPath)) :
                    workspace.child(envVars.expand(projectJsonPath));

            if (runWorkflowAnalysis) {
                AnalyzeOptions analyzeOptions = new AnalyzeOptions();
                analyzeOptions.setProjectPath(expandedProjectJsonPath.getRemote());

                if (useOrchestrator) {
                    analyzeOptions.setOrchestratorUrl(orchestratorAddress);
                    analyzeOptions.setOrchestratorTenant(orchestratorTenant);

                    util.setCredentialsFromCredentialsEntry(credentials, analyzeOptions, run);
                }

                util.execute("AnalyzeOptions", analyzeOptions, tempRemoteDir, listener, envVars, launcher, true);
            }

            PackOptions packOptions = new PackOptions();

            packOptions.setDestinationFolder(expandedOutputPath.getRemote());
            packOptions.setProjectPath(expandedProjectJsonPath.getRemote());
            packOptions.setOutputType(outputType);

            if (version instanceof ManualVersionEntry) {
                packOptions.setVersion(envVars.expand(((ManualVersionEntry) version).getVersion().trim()));
            } else if (version instanceof AutoVersionEntry) {
                packOptions.setAutoVersion(true);
            }

            if (useOrchestrator) {
                packOptions.setOrchestratorUrl(orchestratorAddress);
                packOptions.setOrchestratorTenant(orchestratorTenant);

                util.setCredentialsFromCredentialsEntry(credentials, packOptions, run);
            }

            String language = Locale.getDefault().getLanguage();
            String country = Locale.getDefault().getCountry();
            String localization = country.isEmpty() ? language : language + "-" + country;
            packOptions.setLanguage(localization);

            packOptions.setTraceLevel(traceLevel);

            util.execute("PackOptions", packOptions, tempRemoteDir, listener, envVars, launcher, true);
        } catch (URISyntaxException e) {
            e.printStackTrace(listener.getLogger());
            throw new AbortException(e.getMessage());
        } finally {
            try {
                Objects.requireNonNull(tempRemoteDir).deleteRecursive();
            } catch(Exception e) {
                listener.getLogger().println(com.uipath.uipathpackage.Messages.GenericErrors_FailedToDeleteTempPack() + e.getMessage());
                e.printStackTrace(listener.getLogger());
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
    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

    @DataBoundSetter
    public void setRunWorkflowAnalysis(boolean runWorkflowAnalysis) {
        this.runWorkflowAnalysis = runWorkflowAnalysis;
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
     * Provide the project version
     *
     * @return Entry for versioning
     */
    public SelectEntry getVersion() {
        return version;
    }

    /**
     * Provides the project json path
     *
     * @return String projectJsonPath
     */
    public String getProjectJsonPath() {
        return projectJsonPath;
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
     * @return boolean orchestratorAddress
     */
    public String getOrchestratorAddress() {
        return orchestratorAddress;
    }

    /**
     * Orchestrator tenant
     *
     * @return boolean orchestratorTenant
     */
    public String getOrchestratorTenant() {
        return orchestratorTenant;
    }

    /**
     * Credentials
     *
     * @return boolean credentials
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

    /**
     * Provides the Output Type
     *
     * @return String outputType
     */
    public String getOutputType() {
        return outputType;
    }

    /**
     * Provides the run workflow analysis flag
     *
     * @return boolean runWorkflowAnalysis
     */
    public boolean getRunWorkflowAnalysis() {
        return runWorkflowAnalysis;
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
        if (version == null) {
            throw new InvalidParameterException(com.uipath.uipathpackage.Messages.GenericErrors_MissingVersioningMethod());
        }

        util.validateParams(projectJsonPath, com.uipath.uipathpackage.Messages.ValidationErrors_InvalidProject());
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
    @Symbol("UiPathPack")
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
         * Provides the list of descriptors to the choice in hetero-radio
         *
         * @return list of the Entry descriptors
         */
        public List<Descriptor> getEntryDescriptors() {
            Jenkins jenkins = Jenkins.getInstanceOrNull();
            if (jenkins == null) {
                return new ArrayList<>();
            }

            List<Descriptor> list = new ArrayList<>();
            Descriptor autoDescriptor = jenkins.getDescriptor(AutoVersionEntry.class);
            if (autoDescriptor != null) {
                list.add(autoDescriptor);
            }
            Descriptor manualDescriptor = jenkins.getDescriptor(ManualVersionEntry.class);
            if (manualDescriptor != null) {
                list.add(manualDescriptor);
            }
            Descriptor currentEntryDescriptor = jenkins.getDescriptor(CurrentVersionEntry.class);
            if (currentEntryDescriptor != null) {
                list.add(currentEntryDescriptor);
            }
            return ImmutableList.copyOf(list);
        }

        /**
         * Validated the Project(s) path
         *
         * @param value Project Json Path value
         * @return FormValidation
         */
        public FormValidation doCheckProjectJsonPath(@QueryParameter String value) {
            if (value.trim().isEmpty()) {
                return FormValidation.error(com.uipath.uipathpackage.Messages.UiPathPack_DescriptorImpl_Error_MissingProjectJsonPath());
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
            return com.uipath.uipathpackage.Messages.UiPathPack_DescriptorImpl_DisplayName();
        }

        /**
         * Returns the list of Strings to be filled in choice
         * If item is null or doesn't have configure permission it will return empty list
         *
         * @param item Basic configuration unit in Hudson
         * @return ListBoxModel list of String
         */
        public ListBoxModel doFillOutputTypeItems(@AncestorInPath Item item) {
            if (item == null || !item.hasPermission(Item.CONFIGURE)) {
                return new ListBoxModel();
            }

            ListBoxModel result= new ListBoxModel();
            for (Map.Entry<String, String> v: OutputType.outputTypes.entrySet()) {
                result.add(v.getKey(), v.getValue());
            }

            return result;
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

            ListBoxModel result= new ListBoxModel();
            for (TraceLevel v: TraceLevel.values()) {
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
