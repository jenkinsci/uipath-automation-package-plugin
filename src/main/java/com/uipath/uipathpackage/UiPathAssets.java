package com.uipath.uipathpackage;

import com.google.common.collect.ImmutableList;
import com.uipath.uipathpackage.entries.SelectEntry;
import com.uipath.uipathpackage.entries.assetsAction.DeployAssetsEntry;
import com.uipath.uipathpackage.entries.assetsAction.DeleteAssetsEntry;
import com.uipath.uipathpackage.entries.authentication.ExternalAppAuthenticationEntry;
import com.uipath.uipathpackage.entries.authentication.TokenAuthenticationEntry;
import com.uipath.uipathpackage.entries.authentication.UserPassAuthenticationEntry;
import com.uipath.uipathpackage.util.TraceLevel;
import com.uipath.uipathpackage.util.Utility;
import com.uipath.uipathpackage.models.AssetsOptions;
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
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.*;

import static hudson.slaves.WorkspaceList.tempDir;

public class UiPathAssets extends Builder implements SimpleBuildStep {
    private final Utility util = new Utility();
    private final SelectEntry assetsAction;
    private final String orchestratorAddress;
    private final String orchestratorTenant;
    private final SelectEntry credentials;
    private final String folderName;
    private final String filePath;
    private final TraceLevel traceLevel;

    /**
     * Data bound constructor responsible for setting the values param values to state
     * 
     * @param assetsAction          What to do with the assets: deploy or update.
     * @param orchestratorAddress   Address of the orchestrator
     * @param orchestratorTenant    Tenant on which the task needs to run
     * @param folderName            Name of the folder in which the asset needs to updated
     * @param credentials           credentials to make a connection
     * @param filePath              json filePath
     * @param traceLevel            The trace logging level. One of the following values: None, Critical, Error, Warning, Information, Verbose. (default None)
     *
     */
    @DataBoundConstructor
    public UiPathAssets(SelectEntry assetsAction,
                        String orchestratorAddress,
                        String orchestratorTenant,
                        String folderName,
                        SelectEntry credentials,
                        String filePath,
                        TraceLevel traceLevel) {
        this.assetsAction = assetsAction;
        this.orchestratorAddress = orchestratorAddress;
        this.orchestratorTenant = orchestratorTenant;
        this.folderName = folderName;
        this.credentials = credentials;
        this.filePath = filePath;
        this.traceLevel = traceLevel;
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
    public void perform(@Nonnull Run<?, ?> run,
                        @Nonnull FilePath workspace,
                        @Nonnull EnvVars env,
                        @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener) throws InterruptedException, IOException {
        validateParameters();
        PrintStream logger = listener.getLogger();

        FilePath tempRemoteDir = tempDir(workspace);
        /**
         * Adding the null check here as above method "tempDir" is annotated with @CheckForNull
         * and findbugs plugin will report an error of NPE while building the plugin.
         */
        if (Objects.isNull(tempRemoteDir)) {
            throw new AbortException(com.uipath.uipathpackage.Messages.GenericErrors_FailedToCreateTempFolderAssets());
        }
        tempRemoteDir.mkdirs();

        if (launcher.isUnix()) {
            throw new AbortException(com.uipath.uipathpackage.Messages.GenericErrors_MustUseWindows());
        }

        try {
            EnvVars envVars = run.getEnvironment(listener);

            FilePath expandedCsvFilePath = filePath.contains("${WORKSPACE}") ?
                    new FilePath(launcher.getChannel(), envVars.expand(filePath)) :
                    workspace.child(envVars.expand(filePath));

            AssetsOptions assetsOptions = new AssetsOptions();
            assetsOptions.setOrchestratorUrl(orchestratorAddress);
            String organizationUnit = envVars.expand(folderName.trim());
            assetsOptions.setOrganizationUnit(organizationUnit.length() > 0 ? organizationUnit : "Default");
            assetsOptions.setAssetsFile(expandedCsvFilePath.getRemote());

            ResourceBundle rb = ResourceBundle.getBundle("config");
            String orchestratorTenantFormatted = envVars.expand(orchestratorTenant.trim()).isEmpty()
                                                ? util.getConfigValue(rb, "UiPath.DefaultTenant")
                                                : envVars.expand(orchestratorTenant.trim());

            assetsOptions.setOrchestratorTenant(orchestratorTenantFormatted);
            util.setCredentialsFromCredentialsEntry(credentials, assetsOptions, run);
            String assetAction = assetsAction instanceof DeployAssetsEntry
                                ? "DeployAssetsOptions"
                                : assetsAction instanceof DeleteAssetsEntry
                                    ? "DeleteAssetsOptions"
                                    : "None";

            String language = Locale.getDefault().getLanguage();
            String country = Locale.getDefault().getCountry();
            String localization = country.isEmpty() ? language : language + "-" + country;
            assetsOptions.setLanguage(localization);

            assetsOptions.setTraceLevel(traceLevel);

            if (assetAction.equals("None")) {
                throw new AbortException(com.uipath.uipathpackage.Messages.GenericErrors_InvalidAction());
            }

            int result = util.execute(assetAction, assetsOptions, tempRemoteDir, listener, envVars, launcher, true);

            if (result != 0) {
                throw new AbortException(com.uipath.uipathpackage.Messages.GenericErrors_FailedToRunCommand());
            }
        } catch (URISyntaxException e) {
            e.printStackTrace(logger);
            throw new AbortException(e.getMessage());
        } finally {
            try {
                Objects.requireNonNull(tempRemoteDir).deleteRecursive();
            } catch(Exception e) {
                logger.println(com.uipath.uipathpackage.Messages.GenericErrors_FailedToDeleteTempAssets() + e.getMessage());
                e.printStackTrace(logger);
            }
        }
    }

    /**
     * Getter for assetsAction
     * 
     * @return The selected SelectEntry that was selected by the user.
     */
    public SelectEntry getAssetsAction() {
        return assetsAction;
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
     * CSV File Path
     *
     * @return String filePath
     */
    public String getFilePath() {
        return filePath;
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
        util.validateParams(filePath, com.uipath.uipathpackage.Messages.ValidationErrors_InvalidPackage());
        util.validateParams(orchestratorAddress, com.uipath.uipathpackage.Messages.ValidationErrors_InvalidOrchAddress());

        if (credentials == null) {
            throw new InvalidParameterException(com.uipath.uipathpackage.Messages.ValidationErrors_InvalidCredentialsType());
        }

        credentials.validateParameters();

        if (filePath.toUpperCase().contains("${JENKINS_HOME}")) {
            throw new AbortException(com.uipath.uipathpackage.Messages.ValidationErrors_InvalidPath());
        }
    }

    /**
     * {@link Descriptor} for {@link Builder}
     */
    @Symbol("UiPathAssets")
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
         * Provides the list of descriptors to the choice assetsAction in hetero-radio
         *
         * @return list of the Entry descriptors
         */
        public List<Descriptor> getEntryDescriptors() {
            Jenkins jenkins = Jenkins.getInstance();
            List<Descriptor> list = new ArrayList<>();
            Descriptor deployAssetsDescriptor = jenkins.getDescriptor(DeployAssetsEntry.class);
            if (deployAssetsDescriptor != null) {
                list.add(deployAssetsDescriptor);
            }
            Descriptor deleteAssetsDescriptor = jenkins.getDescriptor(DeleteAssetsEntry.class);
            if (deleteAssetsDescriptor != null) {
                list.add(deleteAssetsDescriptor);
            }
            return ImmutableList.copyOf(list);
        }

        /**
         * Validates CSV File Path
         *
         * @param value value of csv file path
         * @return FormValidation
         */
        public FormValidation doCheckFilePath(@QueryParameter String value) {
            if (value.trim().isEmpty()) {
                return FormValidation.error(com.uipath.uipathpackage.Messages.UiPathAssets_DescriptorImpl_Errors_MissingFilePath());
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
         * Provides the list of descriptors to the choice credentials in hetero-radio
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
         * Provides the display name to the build step
         *
         * @return String display name
         */
        @Nonnull
        @Override
        public String getDisplayName() {
            return com.uipath.uipathpackage.Messages.UiPathAssets_DescriptorImpl_DisplayName();
        }
    }
}