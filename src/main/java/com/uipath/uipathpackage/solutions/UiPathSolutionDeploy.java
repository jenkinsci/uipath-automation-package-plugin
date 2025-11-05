package com.uipath.uipathpackage.solutions;

import com.google.common.collect.ImmutableList;
import com.uipath.uipathpackage.entries.SelectEntry;
import com.uipath.uipathpackage.entries.authentication.ExternalAppAuthenticationEntry;
import com.uipath.uipathpackage.entries.authentication.TokenAuthenticationEntry;
import com.uipath.uipathpackage.entries.authentication.UserPassAuthenticationEntry;
import com.uipath.uipathpackage.models.solutions.SolutionDeployOptions;
import com.uipath.uipathpackage.util.CliDetails;
import com.uipath.uipathpackage.util.TraceLevel;
import com.uipath.uipathpackage.util.Utility;
import hudson.*;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static hudson.slaves.WorkspaceList.tempDir;

/**
 * Deploys a solution/package by creating a deployment entity in Orchestrator based on provided parameters.
 */
public class UiPathSolutionDeploy extends Recorder implements SimpleBuildStep {
    private final String packageName;
    private final String packageVersion;
    private final String deploymentName;
    private final String deploymentFolderName;

    private String deploymentParentFolder;
    private String configPath;

    private final SelectEntry credentials;
    private final TraceLevel traceLevel;
    private final String orchestratorAddress;
    private final String orchestratorTenant;

    @DataBoundConstructor
    public UiPathSolutionDeploy(String packageName,
                                String packageVersion,
                                String deploymentName,
                                String deploymentFolderName,
                                SelectEntry credentials,
                                TraceLevel traceLevel,
                                String orchestratorAddress,
                                String orchestratorTenant) {
        this.packageName = packageName;
        this.packageVersion = packageVersion;
        this.deploymentName = deploymentName;
        this.deploymentFolderName = deploymentFolderName;
        this.credentials = credentials;
        this.traceLevel = traceLevel;
        this.orchestratorAddress = orchestratorAddress;
        this.orchestratorTenant = orchestratorTenant;
    }

    @DataBoundSetter
    public void setDeploymentParentFolder(String deploymentParentFolder) {
        this.deploymentParentFolder = deploymentParentFolder;
    }

    @DataBoundSetter
    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getPackageVersion() {
        return packageVersion;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public String getDeploymentFolderName() {
        return deploymentFolderName;
    }

    public String getDeploymentParentFolder() {
        return deploymentParentFolder;
    }

    public String getConfigPath() {
        return configPath;
    }

    public SelectEntry getCredentials() {
        return credentials;
    }

    public TraceLevel getTraceLevel() {
        return traceLevel;
    }

    public String getOrchestratorAddress() {
        return orchestratorAddress;
    }

    public String getOrchestratorTenant() {
        return orchestratorTenant;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {
        validateParameters();
        PrintStream logger = listener.getLogger();
        FilePath tempRemoteDir = tempDir(workspace);
        if (tempRemoteDir == null) {
            throw new AbortException("Failed to create temp folder for solution deploy.");
        }
        tempRemoteDir.mkdirs();
        Utility util = new Utility();
        try {
            EnvVars envVars = com.uipath.uipathpackage.util.TaskScopedEnvVarsManager.addRequiredEnvironmentVariables(run, env, listener);
            util.validateRuntime(launcher, envVars);
            CliDetails cliDetails = util.getCliDetails(run, listener, envVars, launcher);
            String buildTag = envVars.get("BUILD_TAG");

            FilePath expandedConfigPath = null;
            if (configPath != null && !configPath.trim().isEmpty()) {
                String raw = envVars.expand(configPath.trim());
                expandedConfigPath = raw.contains("${WORKSPACE}") ?
                        new FilePath(launcher.getChannel(), raw) :
                        workspace.child(raw);
            }

            SolutionDeployOptions options = new SolutionDeployOptions();
            if (cliDetails.getActualVersion().supportsNewTelemetry()) {
                options.populateAdditionalTelemetryData();
                options.setPipelineCorrelationId(buildTag);
                options.setCliGetFlow(cliDetails.getGetFlow());
            }

            options.setPackageName(envVars.expand(packageName));
            options.setPackageVersion(envVars.expand(packageVersion));
            options.setDeploymentName(envVars.expand(deploymentName));
            options.setDeploymentFolderName(envVars.expand(deploymentFolderName));
            if (deploymentParentFolder != null && !deploymentParentFolder.trim().isEmpty()) {
                options.setDeploymentParentFolder(envVars.expand(deploymentParentFolder.trim()));
            }
            if (expandedConfigPath != null) {
                options.setConfigPath(expandedConfigPath.getRemote());
            }

            util.setCredentialsFromCredentialsEntry(credentials, options, run);
            options.setTraceLevel(traceLevel);
            options.setOrchestratorUrl(orchestratorAddress);
            options.setOrchestratorTenant(orchestratorTenant);

            String language = Locale.getDefault().getLanguage();
            String country = Locale.getDefault().getCountry();
            String localization = country.isEmpty() ? language : language + "-" + country;
            options.setLanguage(localization);

            int exitCode = util.execute(
                    "SolutionDeployOptions",
                    options,
                    tempRemoteDir,
                    listener,
                    envVars,
                    launcher,
                    true
            );
            if (exitCode != 0) {
                throw new AbortException("UiPath CLI solution deploy failed with exit code: " + exitCode);
            }
            logger.println("Solution deployment completed successfully.");
        } catch (URISyntaxException e) {
            e.printStackTrace(logger);
            throw new AbortException(e.getMessage());
        } finally {
            try {
                if (tempRemoteDir != null) tempRemoteDir.deleteRecursive();
            } catch (Exception e) {
                logger.println("Failed to delete temp folder after solution deploy: " + e.getMessage());
            }
        }
    }

    private void validateParameters() throws AbortException {
        Utility util = new Utility();
        util.validateParams(packageName, "Invalid package name");
        util.validateParams(packageVersion, "Invalid package version");
        util.validateParams(deploymentName, "Invalid deployment name");
        util.validateParams(deploymentFolderName, "Invalid deployment folder name");
        util.validateParams(orchestratorAddress, "Invalid orchestrator address");
        util.validateParams(orchestratorTenant, "Invalid orchestrator tenant");
        if (credentials == null) {
            throw new InvalidParameterException("Invalid credentials type");
        }
    }

    @Symbol("UiPathSolutionDeploy")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "UiPath Solution: Deploy";
        }

        public FormValidation doCheckPackageName(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty()) return FormValidation.error("Package name is required.");
            return FormValidation.ok();
        }

        public FormValidation doCheckPackageVersion(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty()) return FormValidation.error("Package version is required.");
            return FormValidation.ok();
        }

        public FormValidation doCheckDeploymentName(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty()) return FormValidation.error("Deployment name is required.");
            return FormValidation.ok();
        }

        public FormValidation doCheckDeploymentFolderName(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty())
                return FormValidation.error("Solution deployment folder name is required.");
            return FormValidation.ok();
        }

        public FormValidation doCheckOrchestratorAddress(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty())
                return FormValidation.error("Orchestrator address is required.");
            return FormValidation.ok();
        }

        public FormValidation doCheckOrchestratorTenant(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty())
                return FormValidation.error("Orchestrator tenant is required.");
            return FormValidation.ok();
        }

        public List<Descriptor> getAuthenticationDescriptors() {
            Jenkins jenkins = Jenkins.getInstanceOrNull();
            if (jenkins == null) {
                return new ArrayList<>();
            }
            List<Descriptor> list = new ArrayList<>();
            Descriptor userPass = jenkins.getDescriptor(UserPassAuthenticationEntry.class);
            if (userPass != null) list.add(userPass);
            Descriptor token = jenkins.getDescriptor(TokenAuthenticationEntry.class);
            if (token != null) list.add(token);
            Descriptor external = jenkins.getDescriptor(ExternalAppAuthenticationEntry.class);
            if (external != null) list.add(external);
            return ImmutableList.copyOf(list);
        }

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
    }
}
