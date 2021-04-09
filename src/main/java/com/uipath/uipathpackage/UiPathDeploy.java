package com.uipath.uipathpackage;

import com.google.common.collect.ImmutableList;
import com.uipath.uipathpackage.entries.SelectEntry;
import com.uipath.uipathpackage.entries.authentication.TokenAuthenticationEntry;
import com.uipath.uipathpackage.entries.authentication.UserPassAuthenticationEntry;
import com.uipath.uipathpackage.models.DeployOptions;
import com.uipath.uipathpackage.util.TraceLevel;
import com.uipath.uipathpackage.util.Utility;
import hudson.*;
import hudson.model.*;
import hudson.tasks.*;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.*;

import static hudson.slaves.WorkspaceList.tempDir;

/**
 * Class responsible for deploying the nuget package to the orchestrator
 * instance provided by the user.
 */
public class UiPathDeploy extends Recorder implements SimpleBuildStep {
    private final Utility util = new Utility();
    private final String packagePath;
    private final String orchestratorAddress;
    private final String orchestratorTenant;
    private final SelectEntry credentials;
    private final String environments;
    private final String folderName;
    private TraceLevel traceLevel;

    /**
     * Data bound constructor which is responsible for setting/saving of the values
     * provided by the user
     *
     * @param packagePath         Package Path
     * @param orchestratorAddress Orchestrator base URL
     * @param orchestratorTenant  Orchestrator tenant
     * @param folderName          Orchestrator folder
     * @param credentials         Orchestrator credentials
     * @param environments        Environments on which to deploy
     * @param traceLevel          The trace logging level. One of the following values: None, Critical, Error, Warning, Information, Verbose. (default None)
     */
    @DataBoundConstructor
    public UiPathDeploy(String packagePath, String orchestratorAddress, String orchestratorTenant,
            String folderName, String environments, SelectEntry credentials, TraceLevel traceLevel) {
        this.packagePath = packagePath;
        this.orchestratorAddress = orchestratorAddress;
        this.orchestratorTenant = orchestratorTenant;
        this.credentials = credentials;
        this.folderName = folderName;
        this.environments = environments;
        this.traceLevel = traceLevel;
    }

    /**
     * Credentials ID, appearing as choice and will be responsible to extract
     * credentials and use for orchestrator connection
     *
     * @return SelectEntry credentials
     */
    public SelectEntry getCredentials() {
        return credentials;
    }

    /**
     * nupkg path which has to be uploaded
     *
     * @return String packagePath
     */
    public String getPackagePath() {
        return packagePath;
    }

    /**
     * Base orchestrator URL
     *
     * @return String orchestratorAddress
     */
    public String getOrchestratorAddress() {
        return orchestratorAddress;
    }

    /**
     * Orchestrator Tenant
     *
     * @return String orchestratorTenant
     */
    public String getOrchestratorTenant() {
        return orchestratorTenant;
    }

    /**
     * Orchestrator Folder
     *
     * @return String folderName
     */
    public String getFolderName() {
        return folderName;
    }

    /**
     * The comma-separated list of environments which should be assigned to the process or test cases in the package.
     * @return The environments on which to deploy
     */
    public String getEnvironments() {
        return environments;
    }

    /**
     * traceLevel
     *
     * @return TraceLevel traceLevel
     */
    public TraceLevel getTraceLevel() {
        return traceLevel;
    }

    /**
     * Declares the scope of the synchronization monitor this {@link BuildStep} expects from outside.
     * {@link BuildStepMonitor#NONE}
     * No external synchronization is performed on this build step. This is the most efficient, and thus
     * <b>the recommended value for newer plugins</b>. Wherever necessary, you can directly use {@link CheckPoint}s
     * to perform necessary synchronizations.
     *
     * @return BuildStepMonitor BuildStepMonitor.NONE
     */
    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
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
        PrintStream logger = listener.getLogger();

        FilePath tempRemoteDir = tempDir(workspace);
        tempRemoteDir.mkdirs();

        if (launcher.isUnix()) {
            throw new AbortException(com.uipath.uipathpackage.Messages.GenericErrors_MustUseWindows());
        }

        try {
            EnvVars envVars = run.getEnvironment(listener);

            FilePath expandedPackagePath = packagePath.contains("${WORKSPACE}") ?
                    new FilePath(launcher.getChannel(), envVars.expand(packagePath)) :
                    workspace.child(envVars.expand(packagePath));

            DeployOptions deployOptions = new DeployOptions();
            deployOptions.setPackagesPath(expandedPackagePath.getRemote());
            deployOptions.setOrchestratorUrl(orchestratorAddress);
            deployOptions.setOrganizationUnit(envVars.expand(folderName.trim()));

            ResourceBundle rb = ResourceBundle.getBundle("config");
            String orchestratorTenantFormatted = envVars.expand(orchestratorTenant.trim()).isEmpty()
                    ? util.getConfigValue(rb, "UiPath.DefaultTenant") : envVars.expand(orchestratorTenant.trim());
            deployOptions.setOrchestratorTenant(orchestratorTenantFormatted);

            util.setCredentialsFromCredentialsEntry(credentials, deployOptions, run);

            String language = Locale.getDefault().getLanguage();
            String country = Locale.getDefault().getCountry();
            String localization = country.isEmpty() ? language : language + "-" + country;
            deployOptions.setLanguage(localization);

            deployOptions.setTraceLevel(traceLevel);

            if (this.environments != null && !this.environments.isEmpty()) {
                String[] deploymentEnvironments = envVars.expand(this.environments).split(",");
                deployOptions.setEnvironments(Arrays.asList(deploymentEnvironments));
            }
            else {
                deployOptions.setEnvironments(new ArrayList<>());
            }

            util.execute("DeployOptions", deployOptions, tempRemoteDir, listener, envVars, launcher, true);
        } catch (URISyntaxException e) {
            e.printStackTrace(logger);
            throw new AbortException(e.getMessage());
        } finally {
            try {
                Objects.requireNonNull(tempRemoteDir).deleteRecursive();
            } catch(Exception e) {
                logger.println(com.uipath.uipathpackage.Messages.GenericErrors_FailedToDeleteTempDeploy() + e.getMessage());
                e.printStackTrace(logger);
            }
        }
    }

    private void validateParameters() throws AbortException {
        util.validateParams(packagePath, com.uipath.uipathpackage.Messages.ValidationErrors_InvalidPackage());
        util.validateParams(orchestratorAddress, com.uipath.uipathpackage.Messages.ValidationErrors_InvalidOrchAddress());
        util.validateParams(folderName, com.uipath.uipathpackage.Messages.ValidationErrors_InvalidOrchFolder());

        if (credentials == null) {
            throw new InvalidParameterException(com.uipath.uipathpackage.Messages.ValidationErrors_InvalidCredentialsType());
        }

        credentials.validateParameters();

        if (packagePath.toUpperCase().contains("${JENKINS_HOME}")) {
            throw new AbortException(com.uipath.uipathpackage.Messages.ValidationErrors_InvalidPath());
        }
    }

    /**
     * {@link Publisher}.
     */
    @Symbol("UiPathDeploy")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        /**
         * Sets the display name of the build step
         *
         * @return String display name
         */
        @Nonnull
        @Override
        public String getDisplayName() {
            return com.uipath.uipathpackage.Messages.UiPathDeploy_DescriptorImpl_DisplayName();
        }

        /**
         * Returns true if this task is applicable to the given project.
         *
         * @return true to allow user to configure this post-promotion task for the given project.
         * @see AbstractProject.AbstractProjectDescriptor#isApplicable(Descriptor)
         */
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        /**
         * Validates Package(s) Path
         *
         * @param value value of package path
         * @return FormValidation
         */
        public FormValidation doCheckPackagePath(@QueryParameter String value) {
            if (value.trim().isEmpty()) {
                return FormValidation.error(com.uipath.uipathpackage.Messages.UiPathDeploy_DescriptorImpl_Errors_MissingPackagePath());
            }

            if (value.trim().toUpperCase().contains("${JENKINS_HOME}")) {
                return FormValidation.error(com.uipath.uipathpackage.Messages.GenericErrors_MustUseSlavePaths());
            }

            return FormValidation.ok();
        }

        /**
         * Validates Orchestrator Address
         *
         * @param value value of orchestrator address
         * @return FormValidation
         */
        public FormValidation doCheckOrchestratorAddress(@QueryParameter String value) {
            if (value.trim().isEmpty()) {
                return FormValidation.error(com.uipath.uipathpackage.Messages.GenericErrors_MissingOrchestratorAddress());
            }
            return FormValidation.ok();
        }

        /**
         * Validates Orchestrator Folder
         *
         * @param value value of orchestrator folder
         * @return FormValidation
         */
        public FormValidation doCheckFolderName(@QueryParameter String value) {
            if (value.trim().isEmpty()) {
                return FormValidation.error(com.uipath.uipathpackage.Messages.GenericErrors_MissingFolder());
            }
            return FormValidation.ok();
        }

        /**
         * Provides the list of descriptors to the choice in hetero-radio
         *
         * @return list of the authentication descriptors
         */
        public List<Descriptor> getAuthenticationDescriptors() {
            Jenkins jenkins = Jenkins.getInstance();
            List<Descriptor> list = new ArrayList<>();
            Descriptor userPassDescriptor = jenkins.getDescriptor(UserPassAuthenticationEntry.class);
            if (userPassDescriptor != null) {
                list.add(userPassDescriptor);
            }
            Descriptor tokenDescriptor = jenkins.getDescriptor(TokenAuthenticationEntry.class);
            if (tokenDescriptor != null) {
                list.add(tokenDescriptor);
            }
            return ImmutableList.copyOf(list);
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
    }
}
