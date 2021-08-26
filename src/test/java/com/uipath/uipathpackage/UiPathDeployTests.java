package com.uipath.uipathpackage;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.uipath.uipathpackage.entries.authentication.TokenAuthenticationEntry;
import com.uipath.uipathpackage.entries.versioning.AutoVersionEntry;
import com.uipath.uipathpackage.util.TraceLevel;
import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
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

@RunWith(Parameterized.class)
public class UiPathDeployTests {
    private static String orchestratorAddress = "null";
    private static String orchestratorTenant = null;
    private static String description;

    private static String userPassCredentialsId;
    private static String username = null;
    private static String password;

    private static String tokenCredentialsId;
    private static String accountName = null;
    private static String refreshToken;

    private static String externalAppCredentialsId;
    private static String accountForApp;
    private static String applicationId;
    private static String applicationSecret;
    private static String applicationScope;

    private static String environments;
    private static String folderName;
    public static String entryPointPaths;
    private static int folderId;
    private static String workspaceOutputPath;
	private static TraceLevel traceLevel;

    private String packagePath;
    private String packageName;

//    private static UserPassAuthenticationEntry userPassCredentials;
    private static TokenAuthenticationEntry tokenCredentials;
    private static TokenAuthenticationEntry externalAppCredentials;

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
            { new File(Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("ProcessProject")).getPath()).getAbsolutePath(), "ProcessProjectJenkins" },
            { new File(Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("TestProject")).getPath()).getAbsolutePath(), "TestProject" }
        });
    }

    @BeforeClass
    public static void setupClass() {
        orchestratorAddress = System.getenv("TestOrchestratorCloudUrl");
        orchestratorTenant = System.getenv("TestOrchestratorCloudTenant");

        userPassCredentialsId = "TestIdUserPass";
        username = System.getenv("TestOrchestratorUsername");
        password = System.getenv("TestOrchestratorPassword");

        tokenCredentialsId = "TestIdToken";
        accountName = System.getenv("TestOrchestratorAccountName");
        refreshToken = System.getenv("TestOrchestratorAuthenticationToken");
        tokenCredentials = new TokenAuthenticationEntry(tokenCredentialsId, accountName);

        externalAppCredentialsId = "TestIdExternalApp";
        accountForApp = System.getenv("TestOrchestratorAccountName");
        applicationId = System.getenv("TestOrchestratorApplicationId");
        applicationSecret = System.getenv("TestOrchestratorApplicationSecret");
        applicationScope = System.getenv("TestOrchestratorApplicationScope");
        externalAppCredentials = new TokenAuthenticationEntry(externalAppCredentialsId, accountForApp);

        description = "TestDesc";
        environments = System.getenv("TestOrchestratorEnvironments");
        folderName = System.getenv("TestOrchestratorFolderName");
        folderId = 21;
        workspaceOutputPath = "${WORKSPACE}";
        traceLevel = TraceLevel.None;
        entryPointPaths = "Main.xaml,Sequence.xaml";
    }

    @Before
    public void setUp() throws IOException {
//        try {
//            deletePackage();
//        }
//        catch (Exception e) {
//            System.out.println(e);
//        }
        project = jenkins.createFreeStyleProject("freeStyleProject1");
        CredentialsStore store = CredentialsProvider.lookupStores(jenkins).iterator().next();

//        StandardUsernamePasswordCredentials usernamePasswordCredentials = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, userPassCredentialsId, description, username, password);
//        store.addCredentials(Domain.global(), usernamePasswordCredentials);

        StringCredentials tokenCredentials = new StringCredentialsImpl(CredentialsScope.GLOBAL, tokenCredentialsId, description, Secret.fromString(refreshToken));
        store.addCredentials(Domain.global(), tokenCredentials);

        StringCredentials externalAppCredentials = new StringCredentialsImpl(CredentialsScope.GLOBAL, externalAppCredentialsId, description, Secret.fromString(applicationSecret));
        store.addCredentials(Domain.global(), externalAppCredentials);
    }

    @Test
    public void testDeployWithTokenConfigRoundtrip() throws Exception {
        UiPathPack pack = new UiPathPack(new AutoVersionEntry(), packagePath, workspaceOutputPath, traceLevel);
        project.getBuildersList().add(pack);

        UiPathDeploy publisher = new UiPathDeploy(workspaceOutputPath, orchestratorAddress, orchestratorTenant, folderName, environments, tokenCredentials, traceLevel, "");
        project.getPublishersList().add(publisher);
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new UiPathDeploy(workspaceOutputPath, orchestratorAddress, orchestratorTenant, folderName, environments, tokenCredentials, traceLevel, ""), project.getPublishersList().get(0));
    }

    @Test
    public void testDeployWithTokenConfigRoundTrip() throws Exception {
        UiPathPack pack = new UiPathPack(new AutoVersionEntry(), packagePath, workspaceOutputPath, traceLevel);
        project.getBuildersList().add(pack);

        UiPathDeploy publisher = new UiPathDeploy(workspaceOutputPath, orchestratorAddress, orchestratorTenant, folderName, environments, tokenCredentials, traceLevel, "");
        project.getPublishersList().add(publisher);
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new UiPathDeploy(workspaceOutputPath, orchestratorAddress, orchestratorTenant, folderName, environments, tokenCredentials, traceLevel, ""), project.getPublishersList().get(0));
    }

    @Test
    public void testDeployWithExternalAppConfigRoundTrip() throws Exception {
        UiPathPack pack = new UiPathPack(new AutoVersionEntry(), packagePath, workspaceOutputPath, traceLevel);
        project.getBuildersList().add(pack);

        UiPathDeploy publisher = new UiPathDeploy(workspaceOutputPath, orchestratorAddress, orchestratorTenant, folderName, environments, externalAppCredentials, traceLevel, "");
        project.getPublishersList().add(publisher);
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new UiPathDeploy(workspaceOutputPath, orchestratorAddress, orchestratorTenant, folderName, environments, externalAppCredentials, traceLevel, ""), project.getPublishersList().get(0));
    }

    @Test
    public void testExecuteDeployFolderWithTokenReturnsExpectedOutput() throws Exception {
        UiPathPack pack = new UiPathPack(new AutoVersionEntry(), packagePath, workspaceOutputPath, traceLevel);
        project.getBuildersList().add(pack);

        UiPathDeploy publisher = new UiPathDeploy(workspaceOutputPath, orchestratorAddress, orchestratorTenant, folderName, environments, tokenCredentials, traceLevel, "");
        project.getPublishersList().add(publisher);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains(String.format("Deploying project(s)", packagePath), build);
        jenkins.assertLogContains("Deployed", build);
    }

    @Test
    public void testExecuteDeployFileWithTokenReturnsExpectedOutput() throws Exception {
        String nuPkgPath = new FilePath((new File(packagePath)).listFiles()[0]).getRemote();

        UiPathPack pack = new UiPathPack(new AutoVersionEntry(), packagePath, workspaceOutputPath, traceLevel);
        project.getBuildersList().add(pack);

        UiPathDeploy publisher = new UiPathDeploy(workspaceOutputPath, orchestratorAddress, orchestratorTenant, folderName, environments, tokenCredentials, traceLevel, "");
        project.getPublishersList().add(publisher);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains(String.format("Deploying project(s)", nuPkgPath), build);
        jenkins.assertLogContains("Deployed", build);
    }

    @Test
    public void testDeployOnSlave() throws Exception {
        UiPathPack pack = new UiPathPack(new AutoVersionEntry(), packagePath, workspaceOutputPath, traceLevel);
        project.getBuildersList().add(pack);

        UiPathDeploy publisher = new UiPathDeploy(workspaceOutputPath, orchestratorAddress, orchestratorTenant, folderName, environments, tokenCredentials, traceLevel, "");

        DumbSlave node = jenkins.createSlave("aNode", "", null);
        project.setAssignedNode(node);
        project.getPublishersList().add(publisher);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Deployed", build);
    }

    @Test
    public void testUiPathDeployClass() {
        UiPathDeploy uiPathDeploy = new UiPathDeploy(packagePath, orchestratorAddress, orchestratorTenant, folderName, environments, tokenCredentials, traceLevel, "");
        assertEquals(packagePath, uiPathDeploy.getPackagePath());
        assertEquals(orchestratorAddress, uiPathDeploy.getOrchestratorAddress());
        assertEquals(orchestratorTenant, uiPathDeploy.getOrchestratorTenant());
        assertEquals(tokenCredentials, uiPathDeploy.getCredentials());
        assertEquals(folderName, uiPathDeploy.getFolderName());
        assertEquals(traceLevel, uiPathDeploy.getTraceLevel());
        assertEquals("Main.xaml", uiPathDeploy.getEntryPointPaths());
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
        assertEquals(String.valueOf(FormValidation.ok()), String.valueOf(descriptor.doCheckEntryPointPaths(entryPointPaths)));
    }

    @Test
    public void testPublish() throws Exception {
        UiPathPack pack = new UiPathPack(new AutoVersionEntry(), packagePath, workspaceOutputPath, traceLevel);
        project.getBuildersList().add(pack);

        UiPathDeploy publisher = new UiPathDeploy(workspaceOutputPath, orchestratorAddress, orchestratorTenant, folderName, environments, tokenCredentials, traceLevel, "");
        project.getPublishersList().add(publisher);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Deployed", build);
    }

    @Test
    public void testBuildWithWorkspaceRelativePath() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();

        String[] fileNameParts = packagePath.split("\\\\");
        String fileName = fileNameParts[fileNameParts.length-1];

        UiPathPack pack = new UiPathPack(new AutoVersionEntry(), packagePath, workspaceOutputPath, traceLevel);
        project.getBuildersList().add(pack);

        UiPathDeploy publisher = new UiPathDeploy(".", orchestratorAddress, orchestratorTenant, folderName, environments, tokenCredentials, traceLevel, "");
        project.getPublishersList().add(publisher);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        FilePath workspace = project.getSomeBuildWithWorkspace().getWorkspace();
        jenkins.assertLogContains(String.format("Deploying project(s)", workspace.child(fileName).getRemote()), build);
        jenkins.assertLogContains("Deployed", build);
    }

    @Test
    public void testPublishWithEnvVar() throws Exception {
        String nugetPackagePath = "${WORKSPACE}";

        File projectJson = new File(Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("TestProject/project.json")).getPath());
        String projectJsonPath = projectJson.getAbsolutePath();
        UiPathPack builder = new UiPathPack(new AutoVersionEntry(), projectJsonPath, nugetPackagePath, traceLevel);

        project.getBuildersList().add(builder);
        UiPathDeploy publisher = new UiPathDeploy(nugetPackagePath, orchestratorAddress, orchestratorTenant, folderName, environments, tokenCredentials, traceLevel, "");
        project.getPublishersList().add(publisher);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
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
            //assert responseCode == 204;
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
       // assert responseCode == 204;
        postCon.disconnect();
    }
}
