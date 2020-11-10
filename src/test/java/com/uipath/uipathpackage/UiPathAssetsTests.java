package com.uipath.uipathpackage;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.uipath.uipathpackage.entries.authentication.TokenAuthenticationEntry;
import com.uipath.uipathpackage.entries.authentication.UserPassAuthenticationEntry;
import com.uipath.uipathpackage.entries.versioning.AutoVersionEntry;
import com.uipath.uipathpackage.entries.assetsAction.DeployAssetsEntry;
import com.uipath.uipathpackage.entries.assetsAction.UpdateAssetsEntry;
import com.uipath.uipathpackage.entries.assetsAction.DeleteAssetsEntry;
import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.powershell.PowerShell;
import hudson.slaves.DumbSlave;
import hudson.util.FormValidation;
import hudson.util.Secret;
import hudson.AbortException;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;

public class UiPathAssetsTests {

    private static String userPassCredentialsId;
    private static String tokenCredentialsId;
    private static String orchestratorAddress = "null";
    private static String orchestratorTenant = null;
    private static String username = null;
    private static String description;
    private static String password;
    private static String token;
    private static String environments;
    private static String folderName;
    private static int folderId;

    private static UserPassAuthenticationEntry userPassCredentials;
    private static TokenAuthenticationEntry tokenCredentials;
    @Rule
    public final JenkinsRule jenkins = new JenkinsRule();
    private FreeStyleProject project;

    public UiPathAssetsTests() {
    }

    @BeforeClass
    public static void setupClass() {
        orchestratorAddress = System.getenv("TestOrchestratorUrl");
        orchestratorTenant = System.getenv("TestOrchestratorTenant");
        username = System.getenv("TestOrchestratorUsername");
        password = System.getenv("TestOrchestratorPassword");
        token = "testtoken";
        description = "TestDesc";
        userPassCredentialsId = "TestIdUserPass";
        tokenCredentialsId = "TestIdToken";
        environments = System.getenv("TestOrchestratorEnvironments");
        folderName = "Default";
        folderId = 21;
        userPassCredentials = new UserPassAuthenticationEntry(userPassCredentialsId);
        tokenCredentials = new TokenAuthenticationEntry(tokenCredentialsId, "randomaccount");
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
    public void testDeployAssetsWithUsernamePasswordConfigRoundtrip() throws Exception {
        UiPathAssets builder = new UiPathAssets(new DeployAssetsEntry(), orchestratorAddress, orchestratorTenant, folderName, userPassCredentials, "test.csv");
        project.getBuildersList().add(builder);
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new UiPathAssets(new DeployAssetsEntry(), orchestratorAddress, orchestratorTenant, folderName, userPassCredentials, "test.csv"), project.getBuildersList().get(0));
    }

    @Test
    public void testUpdateAssetsWithTokenConfigRoundTrip() throws Exception {
        UiPathAssets builder = new UiPathAssets(new UpdateAssetsEntry(), orchestratorAddress, orchestratorTenant, folderName, userPassCredentials, "test.csv");
        project.getBuildersList().add(builder);
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new UiPathAssets(new UpdateAssetsEntry(), orchestratorAddress, orchestratorTenant, folderName, userPassCredentials, "test.csv"), project.getBuildersList().get(0));
    }

    @Test
    public void testDeployUpdateAssets() throws Exception {
        String assetsResourcesFolder = new File(Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("Assets")).getPath()).getAbsolutePath();
        UiPathAssets builder = new UiPathAssets(new DeployAssetsEntry(), orchestratorAddress, orchestratorTenant, folderName, userPassCredentials, assetsResourcesFolder + "/deploy_test_sample_file.csv");
        project.getBuildersList().add(builder);
        builder = new UiPathAssets(new UpdateAssetsEntry(), orchestratorAddress, orchestratorTenant, folderName, userPassCredentials, assetsResourcesFolder + "/update_test_sample_file.csv");
        project.getBuildersList().add(builder);
        builder = new UiPathAssets(new DeleteAssetsEntry(), orchestratorAddress, orchestratorTenant, folderName, userPassCredentials, assetsResourcesFolder + "/deploy_test_sample_file.csv");
        project.getBuildersList().add(builder);
        project = jenkins.configRoundtrip(project);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
    }
}
