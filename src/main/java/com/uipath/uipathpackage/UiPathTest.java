package com.uipath.uipathpackage;

import com.google.common.collect.ImmutableList;
import com.uipath.uipathpackage.entries.SelectEntry;
import com.uipath.uipathpackage.entries.authentication.ExternalAppAuthenticationEntry;
import com.uipath.uipathpackage.entries.authentication.TokenAuthenticationEntry;
import com.uipath.uipathpackage.entries.authentication.UserPassAuthenticationEntry;
import com.uipath.uipathpackage.entries.testExecutionTarget.TestProjectEntry;
import com.uipath.uipathpackage.entries.testExecutionTarget.TestSetEntry;
import com.uipath.uipathpackage.models.TestOptions;
import com.uipath.uipathpackage.util.*;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.*;
import hudson.model.*;
import hudson.tasks.*;
import hudson.tasks.junit.*;
import hudson.tasks.test.TestResultProjectAction;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

import org.apache.commons.lang.StringUtils;
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
 * Runs a test set or test package on the Orchestrator and outputs the result.
 */
public class UiPathTest extends Recorder implements SimpleBuildStep, JUnitTask {
    private final Utility util = new Utility();
    private final String orchestratorAddress;
    private final String orchestratorTenant;
    private final String folderName;
    private final SelectEntry credentials;
    private final SelectEntry testTarget;
    private final Integer timeout;
    private final String testResultsOutputPath;
    private final String parametersFilePath;
    private String testResultIncludes;
    private final TraceLevel traceLevel;
    private boolean attachRobotLogs;
    
    private static int TimeoutDefault = 7200;

    /**
     * Gets the timeout.
     * @return int timeout
     */
    public Integer getTimeout() {
        return timeout;
    }

    /**
     * Data bound constructor responsible for setting the values param values to state
     *
     * @param orchestratorAddress   UiPath Orchestrator base URL
     * @param orchestratorTenant    UiPath Orchestrator tenant
     * @param folderName            Folder Name
     * @param testTarget            Test target
     * @param credentials           UiPath Orchestrator credentials
     * @param testResultsOutputPath Test result output path (JUnit format)
     * @param timeout               Timeout
     * @param traceLevel            The trace logging level. One of the following values: None, Critical, Error, Warning, Information, Verbose. (default None)
     * @param parametersFilePath    Path of the parameter file
     */
    @DataBoundConstructor
    public UiPathTest(String orchestratorAddress, String orchestratorTenant, String folderName, SelectEntry testTarget, SelectEntry credentials, String testResultsOutputPath, Integer timeout, TraceLevel traceLevel, String parametersFilePath)  {
        this.testTarget = testTarget;
        this.orchestratorAddress = orchestratorAddress;
        this.orchestratorTenant = orchestratorTenant;
        this.folderName = folderName;
        this.credentials = credentials;
        this.timeout = timeout;
        this.testResultsOutputPath = testResultsOutputPath;
		this.parametersFilePath = parametersFilePath;
        this.traceLevel = traceLevel;
        this.attachRobotLogs = false;
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
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @NonNull EnvVars env, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        validateParameters();

        FilePath tempRemoteDir = tempDir(workspace);
        /**
         * Adding the null check here as above method "tempDir" is annotated with @CheckForNull
         * and findbugs plugin will report an error of NPE while building the plugin.
         */
        if (Objects.isNull(tempRemoteDir)) {
            throw new AbortException(com.uipath.uipathpackage.Messages.GenericErrors_FailedToCreateTempFolderTest());
        }

        tempRemoteDir.mkdirs();

        try {
            ResourceBundle rb = ResourceBundle.getBundle("config");
            EnvVars envVars = TaskScopedEnvVarsManager.addRequiredEnvironmentVariables(run, env, listener);
            util.validateRuntime(launcher, envVars);

            CliDetails cliDetails = util.getCliDetails(run, listener, envVars, launcher);
            String buildTag = envVars.get(EnvironmentVariablesConsts.BUILD_TAG);

            TestOptions testOptions = new TestOptions();
            if (cliDetails.getActualVersion().supportsNewTelemetry()) {
                testOptions.populateAdditionalTelemetryData();
                testOptions.setPipelineCorrelationId(buildTag);
                testOptions.setCliGetFlow(cliDetails.getGetFlow());
            }

            if (testTarget instanceof TestProjectEntry)
            {
                String environments = envVars.expand(((TestProjectEntry) testTarget).getEnvironments());
                String projectPath = ((TestProjectEntry) testTarget).getTestProjectPath();
                if (!projectPath.endsWith("project.json")) {
                    if (!projectPath.endsWith("\\")) {
                        projectPath += "\\project.json";
                    } else {
                        projectPath += "project.json";
                    }
                }

                FilePath expandedProjectPath = projectPath.contains("${WORKSPACE}") ?
                        new FilePath(launcher.getChannel(), envVars.expand(projectPath)) :
                        workspace.child(envVars.expand(projectPath));

                testOptions.setProjectPath(expandedProjectPath.getRemote());

                if (environments != null && !environments.trim().isEmpty())
                {
                    testOptions.setEnvironment(environments);
                }
            }
            else {
                testOptions.setTestSet(((TestSetEntry)testTarget).getTestSet());
            }

            String orchestratorTenantFormatted = envVars.expand(orchestratorTenant.trim()).isEmpty() ? util.getConfigValue(rb, "UiPath.DefaultTenant") : envVars.expand(orchestratorTenant.trim());
            testOptions.setOrchestratorUrl(orchestratorAddress);
            testOptions.setOrchestratorTenant(orchestratorTenantFormatted);
            testOptions.setOrganizationUnit(envVars.expand(folderName.trim()));
            testOptions.setTestReportType("junit");

            String resultsOutputPath = testResultsOutputPath != null && !testResultsOutputPath.trim().isEmpty()
                                       ? testResultsOutputPath : "UiPathResults.xml";

            FilePath expandedTestResultsOutputPath = resultsOutputPath.contains("${WORKSPACE}") ?
                    new FilePath(launcher.getChannel(), envVars.expand(resultsOutputPath)) :
                    workspace.child(envVars.expand(resultsOutputPath));

            testOptions.setTestReportDestination(expandedTestResultsOutputPath.getRemote());
            testOptions.setTimeout(timeout != null ? timeout : TimeoutDefault);

            util.setCredentialsFromCredentialsEntry(credentials, testOptions, run);

            String language = Locale.getDefault().getLanguage();
            String country = Locale.getDefault().getCountry();
            String localization = country.isEmpty() ? language : language + "-" + country;
            testOptions.setLanguage(localization);

            testOptions.setTraceLevel(traceLevel);

            if (parametersFilePath != null && !parametersFilePath.isEmpty())
            {
                FilePath parametersPath = parametersFilePath.contains("${WORKSPACE}") ?
                        new FilePath(launcher.getChannel(), envVars.expand(parametersFilePath)) :
                        workspace.child(envVars.expand(parametersFilePath));
                parametersPath.mkdirs();

                testOptions.setParametersFilePath(parametersPath.getRemote());
            }
            
            testOptions.setAttachRobotLogs(attachRobotLogs);
            
            int result = util.execute("RunTestsOptions", testOptions, tempRemoteDir, listener, envVars, launcher, false);

            if (result != 0 && !expandedTestResultsOutputPath.exists()) {
                throw new AbortException(com.uipath.uipathpackage.Messages.GenericErrors_FailedToRunCommand());
            }

            String workspacePath = workspace.getRemote();
            this.testResultIncludes = expandedTestResultsOutputPath.getRemote();
            if (this.testResultIncludes.startsWith(workspacePath)) {
                this.testResultIncludes = this.testResultIncludes.substring(workspacePath.length());
                while ((this.testResultIncludes.startsWith("/") || this.testResultIncludes.startsWith("\\")) && this.testResultIncludes.length() > 1) {
                    this.testResultIncludes = this.testResultIncludes.substring(1);
                }
            }

            if (!expandedTestResultsOutputPath.getName().contains(".")) {
                if (!this.testResultIncludes.endsWith("/") && !this.testResultIncludes.endsWith("\\")) {
                    if (launcher.isUnix()) {
                        this.testResultIncludes += "/";
                    } else {
                        this.testResultIncludes += "\\";
                    }
                }

                this.testResultIncludes += "*.xml";
            }

            run.addAction(new TestResultProjectAction(run.getParent()));
            publishTestResults(run, workspace, launcher, listener);
        } catch (URISyntaxException e) {
            e.printStackTrace(listener.getLogger());
            throw new AbortException(e.getMessage());
        } finally {
            try {
                Objects.requireNonNull(tempRemoteDir).deleteRecursive();
            } catch(Exception e) {
                listener.getLogger().println(com.uipath.uipathpackage.Messages.GenericErrors_FailedToDeleteTempTest() + e.getMessage());
                e.printStackTrace(listener.getLogger());
            }
        }
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
     * Test target to be executed on the Orchestrator
     *
     * @return SelectEntry testTarget
     */
    public SelectEntry getTestTarget() {
        return testTarget;
    }

    /**
     * Credentials, appearing as choice and will be responsible to extract credentials and use for orchestrator connection
     *
     * @return SelectEntry credentials
     */
    public SelectEntry getCredentials() {
        return credentials;
    }

    /**
     * Orchestrator URL
     *
     * @return String orchestratorAddress
     */
    public String getOrchestratorAddress() {
        return orchestratorAddress;
    }

    /**
     * Orchestrator Folder
     * @return String folderName
     */
    public String getFolderName() {
        return folderName;
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
     * Test result output path
     *
     * @return String testResultsOutputPath
     */
    public String getTestResultsOutputPath() {
        return testResultsOutputPath;
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
     * parametersFilePath
     *
     * @return String parametersFilePath
     */
    public String getParametersFilePath() {
        return parametersFilePath;
    }
    
    /**
     * attachRobotLogs
     *
     * @return boolean attachRobotLogs
     */
    public boolean getAttachRobotLogs() {
		return attachRobotLogs;
	}

    /**
     * attachRobotLogs
     *
     * @param attachRobotLogs   Boolean field whether to attach the robot logs
     */
    @DataBoundSetter
    public void setAttachRobotLogs(boolean attachRobotLogs) {
    	this.attachRobotLogs = attachRobotLogs;
    }
    
	private void validateParameters() throws AbortException {
        if (testTarget == null) {
            throw new InvalidParameterException(com.uipath.uipathpackage.Messages.GenericErrors_MissingTestSetOrProjectPath());
        }

        testTarget.validateParameters();

        Utility util = new Utility();
        util.validateParams(this.orchestratorAddress, com.uipath.uipathpackage.Messages.ValidationErrors_InvalidOrchAddress());
        util.validateParams(this.folderName, com.uipath.uipathpackage.Messages.ValidationErrors_InvalidOrchFolder());

        if (credentials == null) {
            throw new InvalidParameterException(com.uipath.uipathpackage.Messages.GenericErrors_MissingAuthenticationMethod());
        }

        credentials.validateParameters();

        if (testResultsOutputPath != null && testResultsOutputPath.toUpperCase().contains("${JENKINS_HOME}")) {
            throw new AbortException(com.uipath.uipathpackage.Messages.ValidationErrors_InvalidPath());
        }
    }

    private void publishTestResults(Run<?,?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        try {
            TestResultSummary resultSummary = JUnitResultArchiver.parseAndSummarize(this, null, run, workspace, launcher, listener);
            if (resultSummary != null) {
                TestResultAction action = run.getAction(TestResultAction.class);
                if(action != null && StringUtils.isNotEmpty(action.getResult().getStdout())) {
                    String stdOut = action.getResult().getStdout();
                    listener.getLogger().println(Messages.UiPathTest_DescriptorImpl_TestRunUrl()+stdOut.substring(stdOut.indexOf("ms.")+3,stdOut.length()));
                }
                if (resultSummary.getFailCount() > 0) {
                    run.setResult(Result.UNSTABLE);
                }
            }
        } catch (Exception e) {
            listener.getLogger().println(e.getMessage());
            throw e;
        }
    }

    @Override
    public String getTestResults() {
        return this.testResultIncludes;
    }

    @Override
    public double getHealthScaleFactor() {
        return 1.0;
    }

    @Override
    public List<TestDataPublisher> getTestDataPublishers() {
        return Collections.emptyList();
    }

    @Override
    public boolean isKeepLongStdio() {
        return true;
    }

    @Override
    public boolean isAllowEmptyResults() {
        return true;
    }

    // Add @Override once we switch to minimum JUnit plugin version 1.4x
    public boolean isSkipPublishingChecks() {
        return false;
    }

    // Add @Override once we switch to minimum JUnit plugin version 1.4x
    public String getChecksName() {
        return "UiPath Tests";
    }

    @Override
    public boolean isSkipOldReports() {
        return false;
    }

    /**
     * {@link Descriptor} for {@link Builder}
     */
    @Symbol("UiPathTest")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

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
         * Provides the display name to the build step
         *
         * @return String display name
         */
        @Nonnull
        @Override
        public String getDisplayName() {
            return com.uipath.uipathpackage.Messages.UiPathTest_DescriptorImpl_DisplayName();
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

            Descriptor testSetDescriptor = jenkins.getDescriptor(TestSetEntry.class);
            if (testSetDescriptor != null) {
                list.add(testSetDescriptor);
            }

            Descriptor testPackageDescriptor = jenkins.getDescriptor(TestProjectEntry.class);
            if (testPackageDescriptor != null) {
                list.add(testPackageDescriptor);
            }

            return ImmutableList.copyOf(list);
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
         * Validates ParametersFilePath
         *
         * @param value value of parameters file path
         * @return FormValidation
         */
        public FormValidation doCheckParametersFilePath(@QueryParameter String value) {
            if (value.trim().toUpperCase().contains("${JENKINS_HOME}")) {
                return FormValidation.error(Messages.GenericErrors_MustUseSlavePaths());
            }

            return FormValidation.ok();
        }
        
        /**
         * Validates that the timeout is specified
         *
         * @param item  Basic configuration unit in Hudson
         * @param value Environments
         * @return FormValidation
         */
        public FormValidation doCheckTimeout(@AncestorInPath Item item, @QueryParameter String value) {
            if (value != null && !value.isEmpty()) {
                try {
                    if (Integer.parseInt(value) < 0) {
                        return FormValidation.error(com.uipath.uipathpackage.Messages.GenericErrors_InvalidTimeout());
                    }
                } catch (NumberFormatException e) {
                    return FormValidation.error(com.uipath.uipathpackage.Messages.GenericErrors_InvalidTimeout());
                }
            }

            return FormValidation.ok();
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
