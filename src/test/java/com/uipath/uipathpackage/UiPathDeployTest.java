package com.uipath.uipathpackage;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.FormValidation;
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

public class UiPathDeployTest {

    private static String credentialsId;
    @Rule
    public final JenkinsRule jenkins = new JenkinsRule();

    private final String packageName = "TestProject.1.0.6987.24350.nupkg";

    private static String orchestratorAddress = "https://platform.uipath.com";
    private static String orchestratorTenant = null;
    private static String username = null;
    private static String description;
    private static String password;
    private static String packagePath = null;

    private FreeStyleProject project;

    @BeforeClass
    public static void setupClass() {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();

        orchestratorAddress = "https://platform.uipath.com";
        orchestratorTenant = "JenkinsTenant";
        username = "admin";
        password = "admin123";
        description = "TestDesc";
        credentialsId = "TestId";
        packagePath = new File(Objects.requireNonNull(classLoader.getResource("TestProject")).getPath()).getAbsolutePath();
    }

    @Before
    public void setUp() throws IOException {
        deletePackage();
        project = jenkins.createFreeStyleProject("freeStyleProject");
        StandardUsernamePasswordCredentials cred = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credentialsId, description, username, password);
        CredentialsProvider.lookupStores(jenkins).iterator().next().addCredentials(Domain.global(), cred);
    }

    @Test
    public void testDeployConfigRoundtrip() throws Exception {
        UiPathDeploy publisher = new UiPathDeploy(packagePath, orchestratorAddress, orchestratorTenant, credentialsId);
        project.getPublishersList().add(publisher);
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new UiPathDeploy(packagePath, orchestratorAddress, orchestratorTenant, credentialsId), project.getPublishersList().get(0));
    }

    @Test
    public void testPublish() throws Exception {
        UiPathDeploy publisher = new UiPathDeploy(packagePath, orchestratorAddress, orchestratorTenant, credentialsId);
        project.getPublishersList().add(publisher);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Deploying", build);
        jenkins.assertLogContains("nupkg successfully added", build);
    }

    @Test
    public void testPublishWithSpecialChar() throws Exception {
        StandardUsernamePasswordCredentials cred = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credentialsId, description, "admin$admin", "admin$admin123");
        CredentialsProvider.lookupStores(jenkins).iterator().next().addCredentials(Domain.global(), cred);
        UiPathDeploy publisher = new UiPathDeploy(packagePath, orchestratorAddress, orchestratorTenant, credentialsId);
        project.getPublishersList().add(publisher);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Deploying", build);
        jenkins.assertLogContains("nupkg successfully added", build);
    }

    @Test
    public void testPublishWithSpecialChar2() throws Exception {
        StandardUsernamePasswordCredentials cred = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credentialsId, description, "admin'admin", "admin'admin123");
        CredentialsProvider.lookupStores(jenkins).iterator().next().addCredentials(Domain.global(), cred);
        UiPathDeploy publisher = new UiPathDeploy(packagePath, orchestratorAddress, orchestratorTenant, credentialsId);
        project.getPublishersList().add(publisher);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Deploying", build);
        jenkins.assertLogContains("nupkg successfully added", build);
    }

    @Test
    public void testPublishWithSpecialChar3() throws Exception {
        StandardUsernamePasswordCredentials cred = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credentialsId, description, "admin\\admin", "admin\\admin123");
        CredentialsProvider.lookupStores(jenkins).iterator().next().addCredentials(Domain.global(), cred);
        UiPathDeploy publisher = new UiPathDeploy(packagePath, orchestratorAddress, orchestratorTenant, credentialsId);
        project.getPublishersList().add(publisher);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Deploying", build);
        jenkins.assertLogContains("nupkg successfully added", build);
    }

    @Test
    public void testPublishWithProjectId() throws Exception {
        String nugetPackagePath = packagePath + "\\" + packageName;
        UiPathDeploy publisher = new UiPathDeploy(nugetPackagePath, orchestratorAddress, orchestratorTenant, credentialsId);
        project.getPublishersList().add(publisher);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Deploying", build);
        jenkins.assertLogContains(packageName + " successfully added", build);
    }

    @Test
    public void testPublishWithEnvVar() throws Exception {
        packagePath = "${JENKINS_HOME}\\jobs\\${JOB_NAME}\\builds\\${BUILD_NUMBER}";
        UiPathDeploy publisher = new UiPathDeploy(packagePath, orchestratorAddress, orchestratorTenant, credentialsId);
        project.getPublishersList().add(publisher);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Deploying", build);
    }

    @Test
    public void testUiPathDeployClass() {
        UiPathDeploy uiPathDeploy = new UiPathDeploy(packagePath, orchestratorAddress, orchestratorTenant, credentialsId);
        assertEquals(packagePath, uiPathDeploy.getPackagePath());
        assertEquals(orchestratorAddress, uiPathDeploy.getOrchestratorAddress());
        assertEquals(orchestratorTenant, uiPathDeploy.getOrchestratorTenant());
        assertEquals(credentialsId, uiPathDeploy.getCredentialsId());
    }

    @Test
    public void testDoCheckPackagePath() {
        UiPathDeploy.DescriptorImpl descriptor = new UiPathDeploy.DescriptorImpl();
        assertEquals(String.valueOf(FormValidation.error(Messages.UiPathDeploy_DescriptorImpl_errors_missingPackagePath())), String.valueOf(descriptor.doCheckPackagePath("")));
        assertEquals(String.valueOf(FormValidation.ok()), String.valueOf(descriptor.doCheckPackagePath(packagePath)));
        assertEquals(String.valueOf(FormValidation.error(Messages.UiPathDeploy_DescriptorImpl_errors_missingOrchestratorAddress())), String.valueOf(descriptor.doCheckOrchestratorAddress("")));
        assertEquals(FormValidation.ok(), descriptor.doCheckOrchestratorAddress(orchestratorAddress));
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
        assert responseCode == 204;
        httpCon.disconnect();
    }
}
