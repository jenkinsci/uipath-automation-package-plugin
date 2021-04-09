package com.uipath.uipathpackage;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.uipath.uipathpackage.entries.authentication.TokenAuthenticationEntry;
import com.uipath.uipathpackage.entries.authentication.UserPassAuthenticationEntry;
import com.uipath.uipathpackage.entries.job.*;
import com.uipath.uipathpackage.util.StartProcessDtoJobPriority;
import com.uipath.uipathpackage.util.TraceLevel;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.Secret;
import org.jenkinsci.plugins.envinject.EnvInjectBuilder;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;

public class UiPathRunJobTests {

    private static String userPassCredentialsId;
    private static String orchestratorAddress = "null";
    private static String orchestratorTenant = null;
    private static String username = null;
    private static String description;
    private static String password;
    private static String token;
    private static String folderName;
    private static TraceLevel traceLevel;
    private static NonProductionJobTypeEntry nonProductionJobTypeEntry;
    private static UnattendedJobTypeEntry unattendedJobTypeEntry;

    private static String cloudOrchestratorAddress = "null";
    private static String cloudOrchestratorTenant = null;
    private static String couldModernFolderName;

    private static String tokenCredentialsId;
    private static UserPassAuthenticationEntry userPassCredentials;
    private static TokenAuthenticationEntry tokenCredentials;
    private static TokenAuthenticationEntry cloudTokenCredentials;

    private static String defaultProcessName;
    private static String classicProcessName;
    private static String modernProcessName;
    private static String cloudModernProcessName;

    private static RobotEntry robotStrategy;

    private static DynamicallyEntry defaultDynamicallyStrategy;
    private static DynamicallyEntry userDynamicallyStrategy;
    private static DynamicallyEntry machineDynamicallyStrategy;
    private static DynamicallyEntry completeDynamicallyStrategy;

    @Rule
    public final JenkinsRule jenkins = new JenkinsRule();
    private FreeStyleProject project;

    @BeforeClass
    public static void setupClass() {
        orchestratorAddress = System.getenv("TestOrchestratorUrl");
        orchestratorTenant = System.getenv("TestOrchestratorTenant");
        username = System.getenv("TestOrchestratorUsername");
        password = System.getenv("TestOrchestratorPassword");
        folderName = System.getenv("TestOrchestratorFolderName");

        traceLevel = TraceLevel.None;

        cloudOrchestratorAddress = System.getenv("TestOrchestratorCloudUrl");
        cloudOrchestratorTenant = System.getenv("TestOrchestratorCloudTenant");

        String accountName = System.getenv("TestOrchestratorAccountName");
        token = System.getenv("TestOrchestratorAuthenticationToken");
        couldModernFolderName = System.getenv("TestOrchestratorCloudModernFolderName");

        String userName = System.getenv("TestOrchestratorCloudUserName");
        String machineName = System.getenv("TestOrchestratorCloudMachineName");

        description = "TestDesc";
        userPassCredentialsId = "TestIdUserPass";
        tokenCredentialsId = "TestIdToken";

        String unattendedRobotName = System.getenv("TestOrchestratorCloudUnattendedRobotName");
        robotStrategy = new RobotEntry(unattendedRobotName);
        unattendedJobTypeEntry = new UnattendedJobTypeEntry();
        nonProductionJobTypeEntry = new NonProductionJobTypeEntry();

        userPassCredentials = new UserPassAuthenticationEntry(userPassCredentialsId);
        tokenCredentials = new TokenAuthenticationEntry(tokenCredentialsId, "randomaccount");
        cloudTokenCredentials = new TokenAuthenticationEntry(tokenCredentialsId, accountName);

        defaultDynamicallyStrategy = new DynamicallyEntry(1, "", "");
        userDynamicallyStrategy = new DynamicallyEntry(1, userName, "");
        machineDynamicallyStrategy = new DynamicallyEntry(1, "", machineName);
        completeDynamicallyStrategy = new DynamicallyEntry(2, userName, machineName);

        defaultProcessName = "";
        classicProcessName = "Run job in cli";

        modernProcessName = "Mobile testing run job";
        cloudModernProcessName = "ShortTestCase";
    }

    @Before
    public void setUp() throws IOException {
        project = jenkins.createFreeStyleProject("freeStyleProject1");
        CredentialsStore store = CredentialsProvider.lookupStores(jenkins).iterator().next();

        StandardUsernamePasswordCredentials usernamePasswordCredentials = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, userPassCredentialsId, description, username, password);
        store.addCredentials(Domain.global(), usernamePasswordCredentials);

        StringCredentials tokenCredentials = new StringCredentialsImpl(CredentialsScope.GLOBAL, tokenCredentialsId, description, Secret.fromString(token));
        store.addCredentials(Domain.global(), tokenCredentials);
    }

    @Test
    public void runJobWithUsernamePasswordAndDefaultConfiguration() throws Exception {
        UiPathRunJob publisher = new UiPathRunJob(defaultProcessName, "", StartProcessDtoJobPriority.Normal, defaultDynamicallyStrategy, unattendedJobTypeEntry, "",
                null, true, true, traceLevel,
                orchestratorAddress, orchestratorTenant, folderName, userPassCredentials);

        project.getPublishersList().add(publisher);
        project = jenkins.configRoundtrip(project);

        jenkins.assertEqualDataBoundBeans(new UiPathRunJob(defaultProcessName, "", StartProcessDtoJobPriority.Normal, defaultDynamicallyStrategy, unattendedJobTypeEntry, "",
                null, true, true, traceLevel,
                orchestratorAddress, orchestratorTenant, folderName,userPassCredentials), project.getPublishersList().get(0));
    }

    @Test
    public void runJobWithTokenAndDefaultConfiguration() throws Exception {
        UiPathRunJob publisher = new UiPathRunJob(defaultProcessName, "", StartProcessDtoJobPriority.Normal, defaultDynamicallyStrategy, unattendedJobTypeEntry, "",
                null, true, true, traceLevel,
                orchestratorAddress, orchestratorTenant, folderName, tokenCredentials);

        project.getPublishersList().add(publisher);
        project = jenkins.configRoundtrip(project);

        jenkins.assertEqualDataBoundBeans(new UiPathRunJob(defaultProcessName, "", StartProcessDtoJobPriority.Normal, defaultDynamicallyStrategy, unattendedJobTypeEntry, "",
                null, true, true, traceLevel,
                orchestratorAddress, orchestratorTenant, folderName, tokenCredentials), project.getPublishersList().get(0));
    }

    @Test
    public void runJobWithDynamicallyStrategySpecificNumberOFJobsUserAndMachine() throws Exception {
        UiPathRunJob publisher = new UiPathRunJob(modernProcessName, "", StartProcessDtoJobPriority.Normal, completeDynamicallyStrategy, unattendedJobTypeEntry, "",
                null, true, true, traceLevel,
                cloudOrchestratorAddress, cloudOrchestratorTenant, couldModernFolderName, userPassCredentials);

        project.getPublishersList().add(publisher);
        project = jenkins.configRoundtrip(project);

        jenkins.assertEqualDataBoundBeans(new UiPathRunJob(modernProcessName, "", StartProcessDtoJobPriority.Normal, completeDynamicallyStrategy, unattendedJobTypeEntry, "",
                null, true, true, traceLevel,
                cloudOrchestratorAddress, cloudOrchestratorTenant, couldModernFolderName, userPassCredentials), project.getPublishersList().get(0));
    }

    @Test
    public void runJobWithSpecificStrategy() throws Exception {
        UiPathRunJob publisher = new UiPathRunJob(defaultProcessName, "", StartProcessDtoJobPriority.Normal, robotStrategy, unattendedJobTypeEntry, "",
                null, true, true, traceLevel,
                orchestratorAddress, orchestratorTenant, folderName, userPassCredentials);

        project.getPublishersList().add(publisher);
        project = jenkins.configRoundtrip(project);

        jenkins.assertEqualDataBoundBeans(new UiPathRunJob(defaultProcessName, "", StartProcessDtoJobPriority.Normal, robotStrategy, unattendedJobTypeEntry, "",
                null, true, true, traceLevel,
                orchestratorAddress, orchestratorTenant, folderName,userPassCredentials), project.getPublishersList().get(0));
    }

    @Test
    public void runJobWithDynamicallyStrategySpecificUser() throws Exception {
        UiPathRunJob publisher = new UiPathRunJob(defaultProcessName, "", StartProcessDtoJobPriority.Normal, userDynamicallyStrategy, unattendedJobTypeEntry, "",
                null, true, true, traceLevel,
                orchestratorAddress, orchestratorTenant, folderName, userPassCredentials);

        project.getPublishersList().add(publisher);
        project = jenkins.configRoundtrip(project);

        jenkins.assertEqualDataBoundBeans(new UiPathRunJob(defaultProcessName, "", StartProcessDtoJobPriority.Normal, userDynamicallyStrategy, unattendedJobTypeEntry, "",
                null, true, true, traceLevel,
                orchestratorAddress, orchestratorTenant, folderName,userPassCredentials), project.getPublishersList().get(0));
    }

    @Test
    public void runJobWithDynamicallyStrategySpecificMachine() throws Exception {
        UiPathRunJob publisher = new UiPathRunJob(defaultProcessName, "", StartProcessDtoJobPriority.Normal, machineDynamicallyStrategy, unattendedJobTypeEntry, "",
                null, true, true, traceLevel,
                orchestratorAddress, orchestratorTenant, folderName, userPassCredentials);

        project.getPublishersList().add(publisher);
        project = jenkins.configRoundtrip(project);

        jenkins.assertEqualDataBoundBeans(new UiPathRunJob(defaultProcessName, "", StartProcessDtoJobPriority.Normal, machineDynamicallyStrategy, unattendedJobTypeEntry, "",
                null, true, true, traceLevel,
                orchestratorAddress, orchestratorTenant, folderName, userPassCredentials), project.getPublishersList().get(0));
    }

    @Test
    public void runJobWithResultFile() throws Exception {
        UiPathRunJob publisher = new UiPathRunJob(defaultProcessName, "", StartProcessDtoJobPriority.Normal, defaultDynamicallyStrategy, unattendedJobTypeEntry, "test",
                null, true, true, traceLevel,
                orchestratorAddress, orchestratorTenant, folderName, userPassCredentials);

        project.getPublishersList().add(publisher);
        project = jenkins.configRoundtrip(project);

        jenkins.assertEqualDataBoundBeans(new UiPathRunJob(defaultProcessName, "", StartProcessDtoJobPriority.Normal, defaultDynamicallyStrategy, unattendedJobTypeEntry, "test",
                null, true, true, traceLevel,
                orchestratorAddress, orchestratorTenant, folderName, userPassCredentials), project.getPublishersList().get(0));
    }

    @Test
    public void runJobWithTimeout() throws Exception {
        UiPathRunJob publisher = new UiPathRunJob(defaultProcessName, "", StartProcessDtoJobPriority.Normal, defaultDynamicallyStrategy, unattendedJobTypeEntry, "",
                10000, true, true, traceLevel,
                orchestratorAddress, orchestratorTenant, folderName, userPassCredentials);

        project.getPublishersList().add(publisher);
        project = jenkins.configRoundtrip(project);

        jenkins.assertEqualDataBoundBeans(new UiPathRunJob(defaultProcessName, "", StartProcessDtoJobPriority.Normal, defaultDynamicallyStrategy, unattendedJobTypeEntry, "",
                10000, true, true, traceLevel,
                orchestratorAddress, orchestratorTenant, folderName, userPassCredentials), project.getPublishersList().get(0));
    }

    @Test
    public void runJobClassicFolder() throws Exception {
        UiPathRunJob publisher = new UiPathRunJob(classicProcessName, null, StartProcessDtoJobPriority.High, defaultDynamicallyStrategy, unattendedJobTypeEntry, null,
                10000, true, true, traceLevel,
                orchestratorAddress, orchestratorTenant, folderName, userPassCredentials);

        project.getPublishersList().add(publisher);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        jenkins.assertLogContains("Starting job run", build);
        jenkins.assertLogContains("Finished running job for process with id", build);
        jenkins.assertLogContains("Running jobs... 1 passed, 0 stopped or terminated, 0 total. Waiting for 20s.", build);
    }

    @Test
    public void runJobClassicFolderWithSpecificRobots() throws Exception {
        UiPathRunJob publisher = new UiPathRunJob(classicProcessName, null, StartProcessDtoJobPriority.High, robotStrategy, unattendedJobTypeEntry, null,
                10000, true, true, traceLevel,
                orchestratorAddress, orchestratorTenant, folderName, userPassCredentials);

        project.getPublishersList().add(publisher);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        jenkins.assertLogContains("Starting job run", build);
        jenkins.assertLogContains("Finished running job for process with id", build);
        jenkins.assertLogContains("Running jobs... 1 passed, 0 stopped or terminated, 0 total. Waiting for 20s.", build);
    }

    @Test
    public void runJobModernFolder() throws Exception {
        UiPathRunJob publisher = new UiPathRunJob(cloudModernProcessName, null, StartProcessDtoJobPriority.High, completeDynamicallyStrategy, unattendedJobTypeEntry, null,
                10000, true, true, traceLevel,
                cloudOrchestratorAddress, cloudOrchestratorTenant, couldModernFolderName, cloudTokenCredentials);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("OrchestratorClientIdOverride=");
        stringBuilder.append(System.getenv("TestOrchestratorClientIdOverride"));
        stringBuilder.append("\nOrchestratorAuthorizationUriOverride=");
        stringBuilder.append(System.getenv("TestOrchestratorAuthorizationUriOverride"));

        project.getPublishersList().add(publisher);
        project.getBuildersList().add(new EnvInjectBuilder(null, stringBuilder.toString()));

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        jenkins.assertLogContains("Starting job run", build);
        jenkins.assertLogContains("Finished running job for process with id", build);
        jenkins.assertLogContains("Running jobs... 2 passed, 0 stopped or terminated, 0 total. Waiting for 20s.", build);
    }

    @Test
    public void runJobNonProductionJobTypeModernFolder() throws Exception {
        UiPathRunJob publisher = new UiPathRunJob(cloudModernProcessName, null, StartProcessDtoJobPriority.High, completeDynamicallyStrategy, nonProductionJobTypeEntry, null,
                10000, true, true, traceLevel,
                cloudOrchestratorAddress, cloudOrchestratorTenant, couldModernFolderName, cloudTokenCredentials);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("OrchestratorClientIdOverride=");
        stringBuilder.append(System.getenv("TestOrchestratorClientIdOverride"));
        stringBuilder.append("\nOrchestratorAuthorizationUriOverride=");
        stringBuilder.append(System.getenv("TestOrchestratorAuthorizationUriOverride"));

        project.getPublishersList().add(publisher);
        project.getBuildersList().add(new EnvInjectBuilder(null, stringBuilder.toString()));

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        jenkins.assertLogContains("Starting job run", build);
        jenkins.assertLogContains("Finished running job for process with id", build);
        jenkins.assertLogContains("Running jobs... 2 passed, 0 stopped or terminated, 0 total. Waiting for 20s.", build);
    }

    @Test
    public void runJobNullJobTypeModernFolder() throws Exception {
        UiPathRunJob publisher = new UiPathRunJob(cloudModernProcessName, null, StartProcessDtoJobPriority.High, completeDynamicallyStrategy, null, null,
                10000, true, true, traceLevel,
                cloudOrchestratorAddress, cloudOrchestratorTenant, couldModernFolderName, cloudTokenCredentials);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("OrchestratorClientIdOverride=");
        stringBuilder.append(System.getenv("TestOrchestratorClientIdOverride"));
        stringBuilder.append("\nOrchestratorAuthorizationUriOverride=");
        stringBuilder.append(System.getenv("TestOrchestratorAuthorizationUriOverride"));

        project.getPublishersList().add(publisher);
        project.getBuildersList().add(new EnvInjectBuilder(null, stringBuilder.toString()));

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        jenkins.assertLogContains("Starting job run", build);
        jenkins.assertLogContains("Finished running job for process with id", build);
        jenkins.assertLogContains("Running jobs... 2 passed, 0 stopped or terminated, 0 total. Waiting for 20s.", build);
    }

    @Test
    public void runJobWithCompleteSpecifications() throws Exception {
        UiPathRunJob publisher = new UiPathRunJob(defaultProcessName, "test", StartProcessDtoJobPriority.High, completeDynamicallyStrategy, unattendedJobTypeEntry, "test",
                10000, true, true, traceLevel,
                orchestratorAddress, orchestratorTenant, folderName, userPassCredentials);

        project.getPublishersList().add(publisher);
        project = jenkins.configRoundtrip(project);

        jenkins.assertEqualDataBoundBeans(new UiPathRunJob(defaultProcessName, "test", StartProcessDtoJobPriority.High, completeDynamicallyStrategy, unattendedJobTypeEntry, "test",
                10000, true, true, traceLevel,
                orchestratorAddress, orchestratorTenant, folderName, userPassCredentials), project.getPublishersList().get(0));
    }
}
