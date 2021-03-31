package com.uipath.uipathpackage;

import com.google.common.collect.ImmutableList;
import com.uipath.uipathpackage.entries.SelectEntry;
import com.uipath.uipathpackage.entries.authentication.TokenAuthenticationEntry;
import com.uipath.uipathpackage.entries.authentication.UserPassAuthenticationEntry;
import com.uipath.uipathpackage.entries.job.*;
import com.uipath.uipathpackage.models.JobOptions;
import com.uipath.uipathpackage.util.StartProcessDtoJobPriority;
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
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.*;

import static hudson.slaves.WorkspaceList.tempDir;

/**
 * Class responsible for running a job in orchestrator
 * instance provided by the user.
 */
public class UiPathRunJob extends Recorder implements SimpleBuildStep {
    private final Utility util = new Utility();

    private String processName;
    private String parametersFilePath;
    private StartProcessDtoJobPriority priority;
    private final SelectEntry jobType;

    private SelectEntry strategy;

    private String resultFilePath;
    private Integer timeout;
    private Boolean failWhenJobFails;
    private Boolean waitForJobCompletion;

    private final String orchestratorAddress;
    private final String orchestratorTenant;
    private final SelectEntry credentials;
    private final String folderName;

    /**
     * Data bound constructor which is responsible for setting/saving of the values
     * provided by the user
     * @param processName           Process Name
     * @param parametersFilePath    The full path to a json input file.
     * @param priority              The priority of job runs. One of the following values: Low, Normal, High. (default Normal)
     * @param strategy              Strategy
     * @param resultFilePath        The full path to a json file or a folder where the result json file will be created.
     * @param timeout               The timeout for job executions in seconds. (default 1800)
     * @param failWhenJobFails      The command fails when at least one job fails. (default true)
     * @param waitForJobCompletion  Wait for job runs completion. (default true)
     * @param orchestratorAddress   Orchestrator base URL
     * @param orchestratorTenant    Orchestrator tenant
     * @param folderName            Orchestrator folder
     * @param credentials           Orchestrator credentials
     */
    @DataBoundConstructor
    public UiPathRunJob(String processName, String parametersFilePath, StartProcessDtoJobPriority priority, SelectEntry strategy, SelectEntry jobType, String resultFilePath,
                        Integer timeout, Boolean failWhenJobFails, Boolean waitForJobCompletion,
                        String orchestratorAddress, String orchestratorTenant, String folderName, SelectEntry credentials) {
        this.processName = processName;
        this.parametersFilePath = parametersFilePath;
        this.priority = priority;
        this.jobType = jobType;

        this.strategy = strategy;

        this.resultFilePath = resultFilePath;
        this.timeout = timeout;
        this.failWhenJobFails = failWhenJobFails;
        this.waitForJobCompletion = waitForJobCompletion;

        this.orchestratorAddress = orchestratorAddress;
        this.orchestratorTenant = orchestratorTenant;
        this.credentials = credentials;
        this.folderName = folderName;
    }

    public SelectEntry getJobType() {return this.jobType;}


    @DataBoundSetter
    public void setStrategy(SelectEntry strategy)
    {
        this.strategy = strategy;
    }

    /**
     * @return SelectEntry strategy
     */
    public SelectEntry getStrategy() {
        return strategy;
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


    @DataBoundSetter
    public void setProcessName(String processName)
    {
        this.processName = processName;
    }

    /**
     * process name
     *
     * @return String processName
     */
    public String getProcessName() {
        return processName;
    }

    @DataBoundSetter
    public void setPriority(StartProcessDtoJobPriority priority)
    {
        this.priority = priority;
    }

    /**
     * priority
     *
     * @return StartProcessDtoJobPriority priority
     */
    public StartProcessDtoJobPriority getPriority() {
        return priority;
    }

    @DataBoundSetter
    public void setParametersFilePath(String parametersFilePath)
    {
        this.parametersFilePath = parametersFilePath;
    }

    /**
     * parameters
     *
     * @return String parameters
     */
    public String getParametersFilePath() {
        return parametersFilePath;
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

    @DataBoundSetter
    public void setFailWhenJobFails(Boolean failWhenJobFails)
    {
        this.failWhenJobFails = failWhenJobFails;
    }

    public Boolean getFailWhenJobFails() { return failWhenJobFails; }


    @DataBoundSetter
    public void setWaitForJobCompletion(Boolean waitForJobCompletion)
    {
        this.waitForJobCompletion = waitForJobCompletion;
    }

    public Boolean getWaitForJobCompletion() { return waitForJobCompletion; }

    @DataBoundSetter
    public void setResultFilePath(String resultFilePath) {
        this.resultFilePath = resultFilePath;
    }

    public String getResultFilePath() { return resultFilePath; }

    @DataBoundSetter
    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Integer getTimeout() {
        return timeout;
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

            JobOptions jobOptions = new JobOptions();
            jobOptions.setProcessName(processName);

            if (parametersFilePath != null && !parametersFilePath.isEmpty())
            {
                FilePath parametersPath = parametersFilePath.contains("${WORKSPACE}") ?
                        new FilePath(launcher.getChannel(), envVars.expand(parametersFilePath)) :
                        workspace.child(envVars.expand(parametersFilePath));
                parametersPath.mkdirs();

                jobOptions.setParametersFilePath(parametersPath.getRemote());
            }

            jobOptions.setPriority(priority);
            util.setJobRunFromStrategyEntry(strategy, jobOptions);

            jobOptions.setResultFilePath(resultFilePath);
            jobOptions.setTimeout(timeout);
            jobOptions.setFailWhenJobFails(failWhenJobFails);
            jobOptions.setWaitForJobCompletion(waitForJobCompletion);

            jobOptions.setOrchestratorUrl(orchestratorAddress);
            jobOptions.setOrganizationUnit(envVars.expand(folderName.trim()));
            util.setJobRunFromJobTypeEntry(jobType, jobOptions);

            ResourceBundle rb = ResourceBundle.getBundle("config");
            String orchestratorTenantFormatted = envVars.expand(orchestratorTenant.trim()).isEmpty() ? util.getConfigValue(rb, "UiPath.DefaultTenant") : envVars.expand(orchestratorTenant.trim());
            jobOptions.setOrchestratorTenant(orchestratorTenantFormatted);

            util.setCredentialsFromCredentialsEntry(credentials, jobOptions, run);

            String language = Locale.getDefault().getLanguage();
            String country = Locale.getDefault().getCountry();
            String localization = country.isEmpty() ? language : language + "-" + country;
            jobOptions.setLanguage(localization);

            util.execute("RunJobOptions", jobOptions, tempRemoteDir, listener, envVars, launcher, true);
        } catch (URISyntaxException e) {
            e.printStackTrace(logger);
            throw new AbortException(e.getMessage());
        } finally {
            try {
                Objects.requireNonNull(tempRemoteDir).deleteRecursive();
            } catch(Exception e) {
                logger.println(com.uipath.uipathpackage.Messages.GenericErrors_FailedToDeleteTempRunJob() + e.getMessage());
                e.printStackTrace(logger);
            }
        }
    }

    private void validateParameters() throws AbortException {
        util.validateParams(processName, com.uipath.uipathpackage.Messages.ValidationErrors_InvalidProcess());
        util.validateParams(orchestratorAddress, com.uipath.uipathpackage.Messages.ValidationErrors_InvalidOrchAddress());
        util.validateParams(folderName, com.uipath.uipathpackage.Messages.ValidationErrors_InvalidOrchFolder());

        if (credentials == null)
        {
            throw new InvalidParameterException(com.uipath.uipathpackage.Messages.ValidationErrors_InvalidCredentialsType());
        }

        credentials.validateParameters();
    }

    /**
     * {@link Publisher}.
     */
    @Symbol("UiPathRunJob")
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
            return Messages.UiPathRunJob_DescriptorImpl_DisplayName();
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
         * Validates Process Name
         *
         * @param value value of process name
         * @return FormValidation
         */
        public FormValidation doCheckProcessName(@QueryParameter String value) {
            if (value.trim().isEmpty()) {
                return FormValidation.error(Messages.UiPathRunJob_DescriptorImpl_Errors_MissingProcessName());
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
         * Validates ResultFilePath
         *
         * @param value value of result file path
         * @return FormValidation
         */
        public FormValidation doCheckResultFilePath(@QueryParameter String value) {
            if (value.trim().toUpperCase().contains("${JENKINS_HOME}")) {
                return FormValidation.error(Messages.GenericErrors_MustUseSlavePaths());
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
                return FormValidation.error(Messages.GenericErrors_MissingOrchestratorAddress());
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
                return FormValidation.error(Messages.GenericErrors_MissingFolder());
            }
            return FormValidation.ok();
        }

        /**
         * Returns the list of Priority options to be filled in choice
         * If item is null or doesn't have configure permission it will return empty list
         *
         * @param item Basic configuration unit in Hudson
         * @return ListBoxModel list of StartProcessDtoJobPriority
         */
        public ListBoxModel doFillPriorityItems(@AncestorInPath Item item) {
            if (item == null || !item.hasPermission(Item.CONFIGURE)) {
                return new ListBoxModel();
            }

            StartProcessDtoJobPriority[] startProcessDtoJobPriorityArrayList = StartProcessDtoJobPriority.values();
            ListBoxModel result = new ListBoxModel();

            for (StartProcessDtoJobPriority priority:startProcessDtoJobPriorityArrayList) {
                result.add(priority.toString());
            }

            return result;
        }

        /**
         * Provides the list of descriptors to the choice in hetero-radio
         *
         * @return list of the strategy descriptors
         */
        public List<Descriptor> getStrategyDescriptors() {
            Jenkins jenkins = Jenkins.getInstance();
            List<Descriptor> list = new ArrayList<>();

            // Add dynamically entry option
            Descriptor dynamicallyDescriptor = jenkins.getDescriptor(DynamicallyEntry.class);
            if (dynamicallyDescriptor != null) {
                list.add(dynamicallyDescriptor);
            }

            // Add specific robot entry option
            Descriptor robotDescriptor = jenkins.getDescriptor(RobotEntry.class);
            if (robotDescriptor != null) {
                list.add(robotDescriptor);
            }

            return ImmutableList.copyOf(list);
        }

        /**
         * Provides the list of descriptors to the choice in hetero-radio
         *
         * @return list of the job type descriptors
         */
        public List<Descriptor> getJobTypeDescriptors() {
            Jenkins jenkins = Jenkins.getInstance();
            List<Descriptor> list = new ArrayList<>();

            // Add unattended job type entry option
            Descriptor unattendedDescriptor = jenkins.getDescriptor(UnattendedJobTypeEntry.class);
            if (unattendedDescriptor != null) {
                list.add(unattendedDescriptor);
            }

            // Add unattended job type entry option
            Descriptor nonProductionDescriptor = jenkins.getDescriptor(NonProductionJobTypeEntry.class);
            if (nonProductionDescriptor != null) {
                list.add(nonProductionDescriptor);
            }

            return ImmutableList.copyOf(list);
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
    }
}
