package com.uipath.uipathpackage;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.*;
import hudson.model.*;
import hudson.security.ACL;
import hudson.tasks.*;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.ResourceBundle;

import static com.uipath.uipathpackage.Utility.escapePowerShellString;

/**
 * Class responsible for deploying the nuget package to the orchestrator instance provided by the user.
 */
public class UiPathDeploy extends Recorder implements SimpleBuildStep {

    private final String packagePath;
    private final String orchestratorAddress;
    private final String orchestratorTenant;
    private final String credentialsId;

    /**
     * Data bound constructor which is responsible for setting/saving of the values provided by the user
     *
     * @param packagePath         Package Path
     * @param orchestratorAddress UiPath Orchestrator base URL
     * @param orchestratorTenant  UiPath Orchestrator base URL
     * @param credentialsId       UiPath Orchestrator Credential Id
     */
    @DataBoundConstructor
    public UiPathDeploy(String packagePath, String orchestratorAddress, String orchestratorTenant, String credentialsId) {
        Utility util = new Utility();
        util.validateParams(packagePath, "Invalid Package(s) Path");
        util.validateParams(orchestratorAddress, "Invalid Orchestrator Address");
        util.validateParams(credentialsId, "Invalid Credentials");
        this.packagePath = packagePath;
        this.orchestratorAddress = orchestratorAddress;
        this.orchestratorTenant = orchestratorTenant;
        this.credentialsId = credentialsId;
    }

    /**
     * Credentials ID, appearing as choice and will be responsible to extract credentials and use for orchestrator connection
     *
     * @return String credentialsId
     */
    public String getCredentialsId() {
        return credentialsId;
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
     * Provides base orchestrator URL
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
     * @param run      a build this is running as a part of
     * @param filePath a workspace to use for any file operations
     * @param launcher a way to start processes
     * @param listener a place to send output
     * @throws InterruptedException if the step is interrupted
     * @throws IOException          if something goes wrong
     */
    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        EnvVars env = run.getEnvironment(listener);
        ResourceBundle rb = ResourceBundle.getBundle("config");
        Utility util = new Utility();
        util.validateParams(orchestratorAddress, "Invalid Orchestrator Address");
        util.validateParams(packagePath, "Invalid Package Path");
        String packagePathFormatted = escapePowerShellString(env.expand(packagePath.trim()));
        String orchestratorTenantFormatted = env.expand(orchestratorTenant.trim()).isEmpty() ? util.getValue(rb, "UiPath.DefaultTenant") : escapePowerShellString(env.expand(orchestratorTenant.trim()));
        StandardUsernamePasswordCredentials cred = CredentialsProvider.findCredentialById(credentialsId, StandardUsernamePasswordCredentials.class, run, Collections.emptyList());
        if (cred == null || cred.getUsername().isEmpty() || cred.getPassword().getPlainText().isEmpty()) {
            throw new AbortException("Invalid credentials");
        }
        String username = escapePowerShellString(cred.getUsername()); //not using escapeJava as it will corrupt the username and password
        String password = escapePowerShellString(cred.getPassword().getPlainText());
        listener.getLogger().println("Opening Powershell Session");
        try {
            String importModuleCommands = util.importModuleCommands(listener, env);
            String deployPackCommand = String.format("Deploy -orchestratorAddress %s -tenant %s -username %s -password %s -packagePath %s -authType UserPass", escapePowerShellString(orchestratorAddress), orchestratorTenantFormatted, username, password, packagePathFormatted);
            util.execute(listener, importModuleCommands, deployPackCommand);
            listener.getLogger().println("Exiting Powershell Session");
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace(listener.getLogger());
            throw new AbortException(e.getMessage());
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
            return Messages.UiPathDeploy_DescriptorImpl_DisplayName();
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
         * Returns the list of StandardUsernamePasswordCredentials to be filled in choice
         * If item is null or doesn't have configure permission it will return empty list
         *
         * @param item Basic configuration unit in Hudson
         * @return ListBoxModel list of StandardUsernamePasswordCredentials
         */
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item) {
            if (item == null || !item.hasPermission(Item.CONFIGURE)) {
                return new ListBoxModel();
            }
            return CredentialsProvider.listCredentials(StandardUsernamePasswordCredentials.class, item, ACL.SYSTEM, Collections.emptyList(), CredentialsMatchers.always());
        }

        /**
         * Validates Credentials if exists
         *
         * @param item  Basic configuration unit in Hudson
         * @param value Any conditional parameter(here id of the credential selected)
         * @return FormValidation
         */
        public FormValidation doCheckCredentialsId(@AncestorInPath Item item, @QueryParameter String value) {
            if (CredentialsProvider.listCredentials(StandardUsernamePasswordCredentials.class, item, ACL.SYSTEM, Collections.emptyList(), CredentialsMatchers.withId(value)).isEmpty()) {
                return FormValidation.error(Messages.UiPathDeploy_DescriptorImpl_errors_missingCredentialsId());
            }
            return FormValidation.ok();
        }

        /**
         * Validates Package(s) Path
         *
         * @param value value of package path
         * @return FormValidation
         */
        public FormValidation doCheckPackagePath(@QueryParameter String value) {
            if (value.trim().isEmpty()) {
                return FormValidation.error(Messages.UiPathDeploy_DescriptorImpl_errors_missingPackagePath());
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
                return FormValidation.error(Messages.UiPathDeploy_DescriptorImpl_errors_missingOrchestratorAddress());
            }
            return FormValidation.ok();
        }
    }
}
