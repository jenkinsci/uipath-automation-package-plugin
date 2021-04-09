package com.uipath.uipathpackage;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.uipath.uipathpackage.entries.authentication.TokenAuthenticationEntry;
import com.uipath.uipathpackage.entries.authentication.UserPassAuthenticationEntry;
import com.uipath.uipathpackage.entries.testExecutionTarget.TestProjectEntry;
import com.uipath.uipathpackage.entries.testExecutionTarget.TestSetEntry;
import com.uipath.uipathpackage.util.TraceLevel;
import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.util.FormValidation;
import hudson.util.Secret;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

public class UiPathTestTests {

    private static String userPassCredentialsId;
    private static String tokenCredentialsId;
    private static String orchestratorAddress = "null";
    private static String orchestratorTenant = null;
    private static String username = null;
    private static String description;
    private static String password;
    private static String token;
    private static String folderName;
    private static TraceLevel traceLevel;
    private static UserPassAuthenticationEntry userPassCredentials;
    private static TokenAuthenticationEntry tokenCredentials;
    private static TestSetEntry testSetTarget;
    private static TestProjectEntry testPackageTarget;
    private static TestProjectEntry testPackagePassTarget;
    private static String testSet;
    private static Integer timeout = 7200;

    @Rule
    public final JenkinsRule jenkins = new JenkinsRule();
    private FreeStyleProject project;

    @BeforeClass
    public static void setupClass() {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();

        orchestratorAddress = System.getenv("TestOrchestratorUrl");
        orchestratorTenant = System.getenv("TestOrchestratorTenant");
        username = System.getenv("TestOrchestratorUsername");
        password = System.getenv("TestOrchestratorPassword");
        folderName = System.getenv("TestOrchestratorFolderName");
        token = "testtoken";
        description = "TestDesc";
        traceLevel = TraceLevel.None;

        userPassCredentialsId = "TestIdUserPass";
        tokenCredentialsId = "TestIdToken";

        userPassCredentials = new UserPassAuthenticationEntry(userPassCredentialsId);
        tokenCredentials = new TokenAuthenticationEntry(tokenCredentialsId, "randomaccount");

        testSet = "CliTestAutomation";
        testSetTarget = new TestSetEntry(testSet);

        String projectPath = new FilePath(new File(Objects.requireNonNull(classLoader.getResource("TestProject")).getPath())).child("project.json").getRemote();
        String environments = System.getenv("TestOrchestratorEnvironments");

        testPackageTarget = new TestProjectEntry(projectPath, environments);

        String projectPathAllPassed = new FilePath(new File(Objects.requireNonNull(classLoader.getResource("TestProjectAllPassed")).getPath())).child("project.json").getRemote();
        testPackagePassTarget = new TestProjectEntry(projectPathAllPassed, environments);
    }

    @Before
    public void setUp() throws IOException {
        deletePackage();
        project = jenkins.createFreeStyleProject("freeStyleProject1");
        CredentialsStore store = CredentialsProvider.lookupStores(jenkins).iterator().next();

        StandardUsernamePasswordCredentials usernamePasswordCredentials = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, userPassCredentialsId, description, username, password);
        store.addCredentials(Domain.global(), usernamePasswordCredentials);

        StringCredentials tokenCredentials = new StringCredentialsImpl(CredentialsScope.GLOBAL, tokenCredentialsId, description, Secret.fromString(token));
        store.addCredentials(Domain.global(), tokenCredentials);
    }

    @Test
    public void testTestWithUsernamePasswordAndTestSetConfigRoundTrip() throws Exception {
        UiPathTest publisher = new UiPathTest(orchestratorAddress, orchestratorTenant, folderName, testSetTarget, userPassCredentials, "", timeout, traceLevel);
        project.getPublishersList().add(publisher);
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new UiPathTest(orchestratorAddress, orchestratorTenant, folderName, testSetTarget, userPassCredentials, "", timeout, traceLevel), project.getPublishersList().get(0));
    }

    @Test
    public void testTestWithUsernamePasswordAndTestPackagePathConfigRoundTrip() throws Exception {
        UiPathTest publisher = new UiPathTest(orchestratorAddress, orchestratorTenant, folderName, testPackageTarget, userPassCredentials, "", timeout, traceLevel);
        project.getPublishersList().add(publisher);
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new UiPathTest(orchestratorAddress, orchestratorTenant, folderName, testPackageTarget, userPassCredentials, "", timeout, traceLevel), project.getPublishersList().get(0));
    }

    @Test
    public void testTestWithTokenAndTestSetConfigRoundTrip() throws Exception {
        UiPathTest publisher = new UiPathTest(orchestratorAddress, orchestratorTenant, folderName, testSetTarget, tokenCredentials, "", timeout, traceLevel);
        project.getPublishersList().add(publisher);
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new UiPathTest(orchestratorAddress, orchestratorTenant, folderName, testSetTarget, tokenCredentials, "", timeout, traceLevel), project.getPublishersList().get(0));
    }

    @Test
    public void testTestWithTokenAndTestPackagePathConfigRoundTrip() throws Exception {
        UiPathTest publisher = new UiPathTest(orchestratorAddress, orchestratorTenant, folderName, testPackageTarget, tokenCredentials, "", timeout, traceLevel);
        project.getPublishersList().add(publisher);
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new UiPathTest(orchestratorAddress, orchestratorTenant, folderName, testPackageTarget, tokenCredentials, "", timeout, traceLevel), project.getPublishersList().get(0));
    }

   @Test
   public void testExecuteTestWithTestSetReturnsExpectedOutput() throws Exception {
       UiPathTest publisher = new UiPathTest(orchestratorAddress, orchestratorTenant, folderName, testSetTarget, userPassCredentials, "results.xml", timeout, traceLevel);
       project.getPublishersList().add(publisher);
       FreeStyleBuild build = jenkins.assertBuildStatus(Result.UNSTABLE, project.scheduleBuild2(0));

       jenkins.assertLogContains(String.format("Started test set %s", testSet), build);
       jenkins.assertLogContains("Writing test results of type junit", build);
       jenkins.assertLogContains("workspace\\freeStyleProject1\\results.xml", build);

       assertEquals(true, build.getWorkspace().child("results.xml").exists());
   }

   /* Experiencing issues on orch-testingsol: endlessly waiting for upload background tasks
   @Test
   public void testExecuteTestWithTestPackageReturnsExpectedOutput() throws Exception {
       UiPathTest publisher = new UiPathTest(orchestratorAddress, orchestratorTenant, folderName, testPackageTarget, userPassCredentials, "results.xml", timeout, traceLevel);
       project.getPublishersList().add(publisher);
       FreeStyleBuild build = jenkins.assertBuildStatus(Result.UNSTABLE, project.scheduleBuild2(0));

       jenkins.assertLogContains("Started test set CI_", build);
       jenkins.assertLogContains("Writing test results of type junit", build);
       jenkins.assertLogContains("workspace\\freeStyleProject1\\results.xml", build);

       assertEquals(true, build.getWorkspace().child("results.xml").exists());
   }*/

    @Test
    public void testExecuteTestWithPassingTestPackageReturnsExpectedOutput() throws Exception {
        UiPathTest publisher = new UiPathTest(orchestratorAddress, orchestratorTenant, folderName, testPackagePassTarget, userPassCredentials, "results.xml", timeout, traceLevel);
        project.getPublishersList().add(publisher);
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0));

        jenkins.assertLogContains("Started test set CI_", build);
        jenkins.assertLogContains("Writing test results of type junit", build);
        jenkins.assertLogContains("workspace\\freeStyleProject1\\results.xml", build);

        assertEquals(true, build.getWorkspace().child("results.xml").exists());
    }

    @Test
    public void testUiPathTestClass() {
        UiPathTest uiPathTest = new UiPathTest(orchestratorAddress, orchestratorTenant, folderName, testPackageTarget, userPassCredentials, "", timeout, traceLevel);
        assertEquals(testPackageTarget, uiPathTest.getTestTarget());
        assertEquals(orchestratorAddress, uiPathTest.getOrchestratorAddress());
        assertEquals(orchestratorTenant, uiPathTest.getOrchestratorTenant());
        assertEquals(userPassCredentials, uiPathTest.getCredentials());
        assertEquals(traceLevel, uiPathTest.getTraceLevel());
    }

    @Test
    public void testDescriptorParameterValidation() {
        UiPathTest.DescriptorImpl descriptor = new UiPathTest.DescriptorImpl();
        assertEquals(FormValidation.ok(), descriptor.doCheckOrchestratorAddress(orchestratorAddress));
        assertEquals(String.valueOf(FormValidation.error(com.uipath.uipathpackage.Messages.GenericErrors_MissingOrchestratorAddress())), String.valueOf(descriptor.doCheckOrchestratorAddress("")));
        assertEquals(String.valueOf(FormValidation.error(com.uipath.uipathpackage.Messages.GenericErrors_MissingFolder())), String.valueOf(descriptor.doCheckFolderName("")));

        TestProjectEntry.DescriptorImpl testProjectDescriptor = new TestProjectEntry.DescriptorImpl();
        assertEquals(String.valueOf(FormValidation.error(com.uipath.uipathpackage.Messages.GenericErrors_MissingTestProjectPath())), String.valueOf(testProjectDescriptor.doCheckTestProjectPath(null, "")));
        assertEquals(String.valueOf(FormValidation.ok()), String.valueOf(testProjectDescriptor.doCheckTestProjectPath(null, "a")));

        TestSetEntry.DescriptorImpl testSetDescriptor = new TestSetEntry.DescriptorImpl();
        assertEquals(String.valueOf(FormValidation.error(com.uipath.uipathpackage.Messages.GenericErrors_MissingTestSetName())), String.valueOf(testSetDescriptor.doCheckTestSet(null, "")));
        assertEquals(String.valueOf(FormValidation.ok()), String.valueOf(testSetDescriptor.doCheckTestSet(null, "a")));
    }

    private void deletePackage() throws IOException {
        URL url = new URL(orchestratorAddress + "/api/Account/Authenticate");
        String body = "{\"tenancyName\": \"" + orchestratorTenant + "\",\"usernameOrEmailAddress\": \"" + username + "\",\"password\": \"" + password + "\"}";
        HttpURLConnection postCon = (HttpURLConnection) url.openConnection();
        postCon.setRequestMethod("POST");
        postCon.addRequestProperty("User-Agent", "Mozilla/4.76");
        postCon.setRequestProperty("Content-Type", "application/json");
        postCon.setDoOutput(true);
        OutputStream os = postCon.getOutputStream();
        os.write(body.getBytes(StandardCharsets.UTF_8));
        os.close();
        InputStream in = new BufferedInputStream(postCon.getInputStream());
        String result = org.apache.commons.io.IOUtils.toString(in, StandardCharsets.UTF_8);
        JSONObject jsonObj = new JSONObject(result);
        String token = jsonObj.getString("result");
        in.close();
        postCon.disconnect();

        url = new URL(orchestratorAddress + "/odata/Processes('TestProject')");
        HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
        httpCon.setDoOutput(true);
        httpCon.setRequestProperty("Authorization", "Bearer " + token);
        httpCon.setRequestProperty("Content-Type", "application/json");
        httpCon.addRequestProperty("User-Agent", "Mozilla/4.76");
        httpCon.setRequestMethod("DELETE");
        int responseCode = httpCon.getResponseCode();
//        assert responseCode == 204;
        httpCon.disconnect();
    }
}
