package com.uipath.uipathpackage;

import com.google.common.collect.ImmutableList;
import com.uipath.uipathpackage.entries.SelectEntry;
import com.uipath.uipathpackage.entries.assetsAction.DeployAssetsEntry;
import com.uipath.uipathpackage.entries.assetsAction.UpdateAssetsEntry;
import com.uipath.uipathpackage.entries.assetsAction.DeleteAssetsEntry;
import com.uipath.uipathpackage.entries.authentication.TokenAuthenticationEntry;
import com.uipath.uipathpackage.entries.authentication.UserPassAuthenticationEntry;
import com.uipath.uipathpackage.util.Utility;
import com.uipath.uipathpackage.models.AssetsOptions;
import hudson.*;
import hudson.model.*;
import hudson.tasks.*;
import hudson.remoting.Channel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.File;
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

    /**
     * Data bound constructor responsible for setting the values param values to state
     * 
     * @param assetsAction  What to do with the assets: deploy or update.
     */
    @DataBoundConstructor
    public UiPathAssets(SelectEntry assetsAction, String orchestratorAddress, String orchestratorTenant,
    String folderName, SelectEntry credentials, String filePath) {
        this.assetsAction = assetsAction;
        this.orchestratorAddress = orchestratorAddress;
        this.orchestratorTenant = orchestratorTenant;
        this.folderName = folderName;
        this.credentials = credentials;
        this.filePath = filePath;
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

        try {
            logger.println("Start");
            if (assetsAction instanceof DeployAssetsEntry) {
                logger.println("Deploy some assets");
            }
            if (assetsAction instanceof UpdateAssetsEntry) {
                logger.println("Update some assets");
            }
            if (assetsAction instanceof DeleteAssetsEntry) {
                logger.println("Update some assets");
            }
            logger.println("Orchestrator URL: " + orchestratorAddress);
            logger.println("Orchestrator Tenant: " + orchestratorTenant);
            logger.println("Orchestrator Folder: " + folderName);
            EnvVars envVars = run.getEnvironment(listener);

            FilePath expandedCsvFilePath = filePath.contains("${WORKSPACE}") ?
                    new FilePath(launcher.getChannel(), envVars.expand(filePath)) :
                    workspace.child(envVars.expand(filePath));
            logger.println("expandedFilePath: " + expandedCsvFilePath.getRemote());

            AssetsOptions assetsOptions = new AssetsOptions();
            assetsOptions.setOrchestratorUrl(orchestratorAddress);
            String organizationUnit = envVars.expand(folderName.trim());
            assetsOptions.setOrganizationUnit(organizationUnit.length() > 0 ? organizationUnit : "Default");
            assetsOptions.setAssetsFile(expandedCsvFilePath.getRemote());

            ResourceBundle rb = ResourceBundle.getBundle("config");
            String orchestratorTenantFormatted = envVars.expand(orchestratorTenant.trim()).isEmpty() ? util.getConfigValue(rb, "UiPath.DefaultTenant") : envVars.expand(orchestratorTenant.trim());

            assetsOptions.setOrchestratorTenant(orchestratorTenantFormatted);
            util.setCredentialsFromCredentialsEntry(credentials, assetsOptions, run);
            String assetAction = assetsAction instanceof DeployAssetsEntry ? "DeployAssetsOptions"
                               : assetsAction instanceof UpdateAssetsEntry ? "UpdateAssetsOptions"
                               : assetsAction instanceof DeleteAssetsEntry ? "DeleteAssetsOptions" : "None";

            if (assetAction.equals("None")) {
                throw new AbortException("Invalid assetAction!");
            }

            int result = util.execute(assetAction, assetsOptions, tempRemoteDir, listener, envVars, launcher, true);

            if (result != 0) {
                throw new AbortException("Failed to run the command.");
            }
        } catch (URISyntaxException e) {
            e.printStackTrace(logger);
            throw new AbortException(e.getMessage());
        } finally {
            try{
                Objects.requireNonNull(tempRemoteDir).deleteRecursive();
            }catch(Exception e){
                logger.println("Failed to delete temp remote directory in UiPath Deploy "+ e.getMessage());
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

    private void validateParameters() throws AbortException {
        util.validateParams(filePath, "Invalid Package(s) Path");
        util.validateParams(orchestratorAddress, "Invalid Orchestrator Address");

        if (credentials == null)
        {
            throw new InvalidParameterException("You must specify either a set of credentials or an authentication token");
        }

        credentials.validateParameters();

        if (filePath.toUpperCase().contains("${JENKINS_HOME}")) {
            throw new AbortException("Paths containing JENKINS_HOME are not allowed, use the Archive Artifacts plugin to copy the required files to the build output.");
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
            Descriptor updateAssetsDescriptor = jenkins.getDescriptor(UpdateAssetsEntry.class);
            if (updateAssetsDescriptor != null) {
                list.add(updateAssetsDescriptor);
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