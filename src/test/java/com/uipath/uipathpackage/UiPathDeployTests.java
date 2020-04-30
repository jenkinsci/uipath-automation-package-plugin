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
import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.powershell.PowerShell;
import hudson.slaves.DumbSlave;
import hudson.util.FormValidation;
import hudson.util.Secret;
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

@RunWith(Parameterized.class)
public class UiPathDeployTests {

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

    private String packagePath = null;
    private String packageName = null;

    private static UserPassAuthenticationEntry userPassCredentials;
    private static TokenAuthenticationEntry tokenCredentials;
    @Rule
    public final JenkinsRule jenkins = new JenkinsRule();
    private FreeStyleProject project;

    public UiPathDeployTests(String packagePath, String packageName) {
        this.packagePath = packagePath;
        this.packageName = packageName;
    }

    @Parameters(name = "{1}")
    public static Iterable<Object[]> data() throws Throwable
    {
        return Arrays.asList(new Object[][] {
            { new File(Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("ProcessPackage")).getPath()).getAbsolutePath(), "ProcessProjectJenkins" },
            { new File(Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("TestPackage")).getPath()).getAbsolutePath(), "TestProject" }
        });
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
        folderName = "JenkinsTests";
        folderId = 21;
        userPassCredentials = new UserPassAuthenticationEntry(userPassCredentialsId);
        tokenCredentials = new TokenAuthenticationEntry(tokenCredentialsId, "randomaccount");
    }

    @Before
    public void setUp() throws IOException {
        try {
            deletePackage();
        }
        catch (Exception e) {
            System.out.println(e);
        }
        project = jenkins.createFreeStyleProject("freeStyleProject1");
        CredentialsStore store = CredentialsProvider.lookupStores(jenkins).iterator().next();

        StandardUsernamePasswordCredentials usernamePasswordCredentials = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, userPassCredentialsId, description, username, password);
        store.addCredentials(Domain.global(), usernamePasswordCredentials);

        StringCredentials tokenCredentials = new StringCredentialsImpl(CredentialsScope.GLOBAL, tokenCredentialsId, description, Secret.fromString(token));
        store.addCredentials(Domain.global(), tokenCredentials);
    }

    @Test
    public void testDeployWithUsernamePasswordConfigRoundtrip() throws Exception {
        UiPathDeploy publisher = new UiPathDeploy(packagePath, orchestratorAddress, orchestratorTenant, folderName, environments, userPassCredentials);
        project.getPublishersList().add(publisher);
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new UiPathDeploy(packagePath, orchestratorAddress, orchestratorTenant, folderName, environments, userPassCredentials), project.getPublishersList().get(0));
    }

    @Test
    public void testDeployWithTokenConfigRoundTrip() throws Exception {
        UiPathDeploy publisher = new UiPathDeploy(packagePath, orchestratorAddress, orchestratorTenant, folderName, environments, tokenCredentials);
        project.getPublishersList().add(publisher);
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new UiPathDeploy(packagePath, orchestratorAddress, orchestratorTenant, folderName, environments, tokenCredentials), project.getPublishersList().get(0));
    }

    @Test
    public void testExecuteDeployFolderWithUserPassReturnsExpectedOutput() throws Exception {
        UiPathDeploy publisher = new UiPathDeploy(packagePath, orchestratorAddress, orchestratorTenant, folderName, environments, userPassCredentials);
        project.getPublishersList().add(publisher);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains(String.format("Deploying project(s)", packagePath), build);
        jenkins.assertLogContains(String.format("uploaded to %s", orchestratorAddress), build);
        jenkins.assertLogContains("Deployed", build);
    }

    @Test
    public void testExecuteDeployFileWithUserPassReturnsExpectedOutput() throws Exception {
        String nuPkgPath = new FilePath((new File(packagePath)).listFiles()[0]).getRemote();
        UiPathDeploy publisher = new UiPathDeploy(nuPkgPath, orchestratorAddress, orchestratorTenant, folderName, environments, userPassCredentials);
        project.getPublishersList().add(publisher);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains(String.format("Deploying project(s)", nuPkgPath), build);
        jenkins.assertLogContains(String.format("uploaded to %s", orchestratorAddress), build);
        jenkins.assertLogContains("Deployed", build);
    }

    @Test
    public void testDeployOnSlave() throws Exception {
        UiPathDeploy publisher = new UiPathDeploy(packagePath, orchestratorAddress, orchestratorTenant, folderName, environments, userPassCredentials);

        DumbSlave node = jenkins.createSlave("aNode", "", null);
        project.setAssignedNode(node);
        project.getPublishersList().add(publisher);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains(String.format("uploaded to %s", orchestratorAddress), build);
        jenkins.assertLogContains("Deployed", build);
    }

    @Test
    public void testUiPathDeployClass() {
        UiPathDeploy uiPathDeploy = new UiPathDeploy(packagePath, orchestratorAddress, orchestratorTenant, folderName, environments, userPassCredentials);
        assertEquals(packagePath, uiPathDeploy.getPackagePath());
        assertEquals(orchestratorAddress, uiPathDeploy.getOrchestratorAddress());
        assertEquals(orchestratorTenant, uiPathDeploy.getOrchestratorTenant());
        assertEquals(userPassCredentials, uiPathDeploy.getCredentials());
        assertEquals(folderName, uiPathDeploy.getFolderName());
    }

    @Test
    public void testDescriptorParameterValidation() {
        UiPathDeploy.DescriptorImpl descriptor = new UiPathDeploy.DescriptorImpl();
        assertEquals(String.valueOf(FormValidation.error(com.uipath.uipathpackage.Messages.UiPathDeploy_DescriptorImpl_Errors_MissingPackagePath())), String.valueOf(descriptor.doCheckPackagePath("")));
        assertEquals(String.valueOf(FormValidation.ok()), String.valueOf(descriptor.doCheckPackagePath(packagePath)));
        assertEquals(String.valueOf(FormValidation.error(com.uipath.uipathpackage.Messages.GenericErrors_MissingOrchestratorAddress())), String.valueOf(descriptor.doCheckOrchestratorAddress("")));
        assertEquals(String.valueOf(FormValidation.ok()), String.valueOf(descriptor.doCheckOrchestratorAddress(orchestratorAddress)));
        assertEquals(String.valueOf(FormValidation.error(com.uipath.uipathpackage.Messages.GenericErrors_MissingFolder())), String.valueOf(descriptor.doCheckFolderName("")));
        assertEquals(String.valueOf(FormValidation.ok()), String.valueOf(descriptor.doCheckFolderName(folderName)));
    }

    @Test
    public void testPublish() throws Exception {
        UiPathDeploy publisher = new UiPathDeploy(packagePath, orchestratorAddress, orchestratorTenant, folderName, environments, userPassCredentials);
        project.getPublishersList().add(publisher);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains(String.format("uploaded to %s", orchestratorAddress), build);
        jenkins.assertLogContains("Deployed", build);
    }

    @Test
    public void testBuildWithWorkspaceRelativePath() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();

        String[] fileNameParts = packagePath.split("\\\\");
        String fileName = fileNameParts[fileNameParts.length-1];
        UiPathDeploy publisher = new UiPathDeploy(".", orchestratorAddress, orchestratorTenant, folderName, environments, userPassCredentials);
        project.getBuildersList().add(new PowerShell("Copy-Item -Path \"" + packagePath + "\\*\" -Destination \".\" -Recurse", true, false));
        project.getPublishersList().add(publisher);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        FilePath workspace = project.getSomeBuildWithWorkspace().getWorkspace();
        jenkins.assertLogContains(String.format("Deploying project(s)", workspace.child(fileName).getRemote()), build);
        jenkins.assertLogContains(String.format("uploaded to %s", orchestratorAddress), build);
        jenkins.assertLogContains("Deployed", build);
    }

    @Test
    public void testPublishWithEnvVar() throws Exception {
        String nugetPackagePath = "${WORKSPACE}";

        File projectJson = new File(Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("TestProject/project.json")).getPath());
        String projectJsonPath = projectJson.getAbsolutePath();
        UiPathPack builder = new UiPathPack(new AutoVersionEntry(), projectJsonPath, nugetPackagePath);

        project.getBuildersList().add(builder);
        UiPathDeploy publisher = new UiPathDeploy(nugetPackagePath, orchestratorAddress, orchestratorTenant, folderName, environments, userPassCredentials);
        project.getPublishersList().add(publisher);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains(String.format("uploaded to %s", orchestratorAddress), build);
        jenkins.assertLogContains("Deployed", build);
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

        url = new URL(orchestratorAddress + "/odata/Releases?$filter=tolower(ProcessKey)%20eq%20%27" + packageName + "%27&$select=Id");
        postCon = (HttpURLConnection) url.openConnection();
        postCon.setRequestProperty("Authorization", "Bearer " + token);
        postCon.setRequestProperty("Content-Type", "application/json");
        postCon.addRequestProperty("User-Agent", "Mozilla/4.76");
        postCon.addRequestProperty("X-UIPATH-OrganizationUnitId", String.valueOf(folderId));
        postCon.addRequestProperty("X-UIPATH-TenantName", "Default");
        postCon.setRequestMethod("GET");
        postCon.setDoInput(true);

        in = new BufferedInputStream(postCon.getInputStream());
        result = org.apache.commons.io.IOUtils.toString(in, StandardCharsets.UTF_8);
        jsonObj = new JSONObject(result);
        JSONArray releases = jsonObj.getJSONArray("value");

        if (!releases.isEmpty())
        {
            int releaseId = releases.getJSONObject(0).getInt("Id");
            in.close();
            postCon.disconnect();

            url = new URL(orchestratorAddress + "/odata/Releases(" + releaseId + ")");
            postCon = (HttpURLConnection) url.openConnection();
            postCon.setDoOutput(true);
            postCon.setRequestProperty("Authorization", "Bearer " + token);
            postCon.setRequestProperty("Content-Type", "application/json");
            postCon.addRequestProperty("User-Agent", "Mozilla/4.76");
            postCon.addRequestProperty("X-UIPATH-OrganizationUnitId", String.valueOf(folderId));
            postCon.addRequestProperty("X-UIPATH-TenantName", "Default");
            postCon.setRequestMethod("DELETE");
            int responseCode = postCon.getResponseCode();
            assert responseCode == 204;
            postCon.disconnect();
        }
        else
        {
            in.close();
            postCon.disconnect();
        }

        url = new URL(orchestratorAddress + "/odata/Processes('" + packageName + "')");
        postCon = (HttpURLConnection) url.openConnection();
        postCon.setDoOutput(true);
        postCon.setRequestProperty("Authorization", "Bearer " + token);
        postCon.setRequestProperty("Content-Type", "application/json");
        postCon.addRequestProperty("User-Agent", "Mozilla/4.76");
        postCon.addRequestProperty("X-UIPATH-OrganizationUnitId", String.valueOf(folderId));
        postCon.addRequestProperty("X-UIPATH-TenantName", "Default");
        postCon.setRequestMethod("DELETE");
        int responseCode = postCon.getResponseCode();
        assert responseCode == 204;
        postCon.disconnect();
    }
}
