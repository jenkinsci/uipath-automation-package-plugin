package com.uipath.uipathpackage;

import com.uipath.uipathpackage.entries.authentication.UserPassAuthenticationEntry;
import com.uipath.uipathpackage.entries.versioning.AutoVersionEntry;
import com.uipath.uipathpackage.entries.versioning.CurrentVersionEntry;
import com.uipath.uipathpackage.entries.versioning.ManualVersionEntry;
import com.uipath.uipathpackage.util.Utility;
import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.powershell.PowerShell;
import hudson.slaves.DumbSlave;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import org.hamcrest.CoreMatchers;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.isA;

public class UiPathPackTests {
    private static AutoVersionEntry autoEntry = null;
    private static ManualVersionEntry manualEntry = null;
    private static CurrentVersionEntry currentEntry = null;
    private static String parentProjectPath = null;
    private static String projectJsonPath = null;
    private static String projectPath = null;

    private static String orchestratorAddress = "null";
    private static String orchestratorTenant = null;
    private static UserPassAuthenticationEntry userPassCredentials;

    private static String outputPath = null;
    private static String outputType = null;
    private static String workspaceOutputPath = null;
    private static String masterOutputPath;

    @Rule
    public final JenkinsRule jenkins = new JenkinsRule();
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock
    Utility util;

    @BeforeClass
    public static void setupClass() {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        File parentProject = new File(Objects.requireNonNull(classLoader.getResource(".")).getPath());
        parentProjectPath = parentProject.getAbsolutePath();
        File project = new File(Objects.requireNonNull(classLoader.getResource("TestProject")).getPath());
        projectPath = project.getAbsolutePath();
        File projectJson = new File(Objects.requireNonNull(classLoader.getResource("ProcessProject/project.json")).getPath());
        projectJsonPath = projectJson.getAbsolutePath();
        outputPath = projectPath;
        autoEntry = new AutoVersionEntry();
        currentEntry = new CurrentVersionEntry();
        manualEntry = new ManualVersionEntry("1.2.3.${BUILD_NUMBER}");
        workspaceOutputPath = "${WORKSPACE}";
        masterOutputPath = "${JENKINS_HOME}\\jobs\\${JOB_NAME}\\builds\\${BUILD_NUMBER}";

        orchestratorAddress = System.getenv("TestOrchestratorUrl");
        orchestratorTenant = System.getenv("TestOrchestratorTenant");

        String userPassCredentialsId = "TestIdUserPass";
        userPassCredentials = new UserPassAuthenticationEntry(userPassCredentialsId);
    }

    @Test
    public void testAutoVersionConfigRoundtrip() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new UiPathPack(autoEntry, projectJsonPath, outputPath));
        project = jenkins.configRoundtrip(project);

        Builder descriptor = project.getBuildersList().get(0);
        jenkins.assertEqualDataBoundBeans(new UiPathPack(autoEntry, projectJsonPath, outputPath), descriptor);
    }

    @Test
    public void testManualVersionConfigRoundtrip() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new UiPathPack(manualEntry, projectJsonPath, outputPath));
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new UiPathPack(manualEntry, projectJsonPath, outputPath), project.getBuildersList().get(0));
    }

    @Test
    public void testCurrentVersionConfigRoundtrip() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new UiPathPack(currentEntry, projectJsonPath, outputPath));
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new UiPathPack(currentEntry, projectJsonPath, outputPath), project.getBuildersList().get(0));
    }

    @Test
    public void testBuildWithJson() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        UiPathPack builder = new UiPathPack(autoEntry, projectJsonPath, outputPath);
        project.getBuildersList().add(builder);
        doNothing().when(util).validateParams(isA(String.class), isA(String.class));
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        builder.setCredentials(userPassCredentials);
        builder.setOrchestratorAddress(orchestratorAddress);
        builder.setOrchestratorTenant(orchestratorTenant);
        builder.setOutputType(outputType);

        jenkins.assertLogContains(String.format("Packing project at path %s", projectJsonPath), build);
        jenkins.assertLogContains(String.format("saved to %s", outputPath), build);
        assertThat(new File(outputPath).exists(), CoreMatchers.is(true));
    }

    @Test
    public void testBuildWithParentProject() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        UiPathPack builder = new UiPathPack(autoEntry, parentProjectPath, outputPath);
        builder.setCredentials(userPassCredentials);
        builder.setOrchestratorAddress(orchestratorAddress);
        builder.setOrchestratorTenant(orchestratorTenant);
        builder.setOutputType(outputType);

        doNothing().when(util).validateParams(isA(String.class), isA(String.class));
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains(String.format("Packing project at path %s", parentProjectPath), build);
        jenkins.assertLogContains(String.format("saved to %s", outputPath), build);
    }

    @Test
    public void testBuildWithWorkspaceRelativePath() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();

        UiPathPack builder = new UiPathPack(autoEntry, "TestProject", "Output");
        builder.setCredentials(userPassCredentials);
        builder.setOrchestratorAddress(orchestratorAddress);
        builder.setOrchestratorTenant(orchestratorTenant);
        builder.setOutputType(outputType);

        doNothing().when(util).validateParams(isA(String.class), isA(String.class));
        project.getBuildersList().add(new PowerShell("Copy-Item -Path \"" + parentProjectPath + "\\*\" -Destination \".\" -Recurse", true, false));
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        FilePath workspace = project.getSomeBuildWithWorkspace().getWorkspace();
        jenkins.assertLogContains(String.format("Packing project at path %s", workspace != null ? workspace.child("TestProject").getRemote() : null), build);
        jenkins.assertLogContains(String.format("saved to %s", workspace != null ? workspace.child("Output").getRemote() : null), build);
    }

    @Test
    public void testBuildWithProject() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        UiPathPack builder = new UiPathPack(autoEntry, projectPath, outputPath);
        builder.setCredentials(userPassCredentials);
        builder.setOrchestratorAddress(orchestratorAddress);
        builder.setOrchestratorTenant(orchestratorTenant);
        builder.setOutputType(outputType);

        project.getBuildersList().add(builder);
        doNothing().when(util).validateParams(isA(String.class), isA(String.class));
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains(String.format("Packing project at path %s", projectPath), build);
        jenkins.assertLogContains(String.format("saved to %s", outputPath), build);
    }

    @Test
    public void testBuildWithEnvVar() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        UiPathPack builder = new UiPathPack(autoEntry, projectPath, workspaceOutputPath);
        builder.setCredentials(userPassCredentials);
        builder.setOrchestratorAddress(orchestratorAddress);
        builder.setOrchestratorTenant(orchestratorTenant);
        builder.setOutputType(outputType);

        project.getBuildersList().add(builder);
        doNothing().when(util).validateParams(isA(String.class), isA(String.class));
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains(String.format("Packing project at path %s", projectPath), build);
        jenkins.assertLogNotContains(workspaceOutputPath, build);
    }

    @Test
    public void testBuildWithEnvVarManualEntry() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        UiPathPack builder = new UiPathPack(manualEntry, projectPath, workspaceOutputPath);
        builder.setCredentials(userPassCredentials);
        builder.setOrchestratorAddress(orchestratorAddress);
        builder.setOrchestratorTenant(orchestratorTenant);
        builder.setOutputType(outputType);

        project.getBuildersList().add(builder);
        doNothing().when(util).validateParams(isA(String.class), isA(String.class));
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains(String.format("Packing project at path %s", projectPath), build);
        jenkins.assertLogNotContains(workspaceOutputPath, build);
    }


    @Test
    public void testBuildOnSlave() throws Exception {
        DumbSlave node = jenkins.createSlave("aNode", "", null);
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.setAssignedNode(node);
        UiPathPack builder = new UiPathPack(manualEntry, projectPath, workspaceOutputPath);
        builder.setCredentials(userPassCredentials);
        builder.setOrchestratorAddress(orchestratorAddress);
        builder.setOrchestratorTenant(orchestratorTenant);
        builder.setOutputType(outputType);

        project.getBuildersList().add(builder);
        doNothing().when(util).validateParams(isA(String.class), isA(String.class));
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains(String.format("Packing project at path %s", projectPath), build);
        jenkins.assertLogNotContains(workspaceOutputPath, build);
    }

    @Test
    public void testBuildOnSlaveWithMasterOutputPathThrows() throws Exception {
        DumbSlave node = jenkins.createSlave("aNode", "", null);
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.setAssignedNode(node);
        UiPathPack builder = new UiPathPack(manualEntry, projectPath, masterOutputPath);
        builder.setCredentials(userPassCredentials);
        builder.setOrchestratorAddress(orchestratorAddress);
        builder.setOrchestratorTenant(orchestratorTenant);
        builder.setOutputType(outputType);

        project.getBuildersList().add(builder);
        doNothing().when(util).validateParams(isA(String.class), isA(String.class));
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.FAILURE, project.scheduleBuild2(0));
        jenkins.assertLogContains("Paths containing JENKINS_HOME are not allowed", build);
        jenkins.assertLogNotContains(masterOutputPath, build);
    }

    @Test
    public void testAutoVersionEntry() {
        UiPathPack uipathPack = new UiPathPack(autoEntry, projectPath, outputPath);
        assertEquals(autoEntry, uipathPack.getVersion());
        assertEquals(projectPath, uipathPack.getProjectJsonPath());
        assertEquals(outputPath, uipathPack.getOutputPath());
    }

    @Test
    public void testManualVersionEntry() {
        UiPathPack uipathPack = new UiPathPack(manualEntry, projectPath, outputPath);
        uipathPack.setCredentials(userPassCredentials);
        uipathPack.setOrchestratorAddress(orchestratorAddress);
        uipathPack.setOrchestratorTenant(orchestratorTenant);
        uipathPack.setOutputType(outputType);

        assertEquals(manualEntry, uipathPack.getVersion());
    }

    @Test
    public void testCurrentVersionEntry() {
        UiPathPack uipathPack = new UiPathPack(currentEntry, projectPath, outputPath);
        uipathPack.setCredentials(userPassCredentials);
        uipathPack.setOrchestratorAddress(orchestratorAddress);
        uipathPack.setOrchestratorTenant(orchestratorTenant);
        uipathPack.setOutputType(outputType);

        assertEquals(currentEntry, uipathPack.getVersion());
        assertEquals(projectPath, uipathPack.getProjectJsonPath());
        assertEquals(outputPath, uipathPack.getOutputPath());
    }

    @Test
    public void testUiPathPackClassDescriptor() {
        UiPathPack.DescriptorImpl descriptor = new UiPathPack.DescriptorImpl();
        assertEquals(String.valueOf(FormValidation.error("Output path is mandatory")), String.valueOf(descriptor.doCheckOutputPath("")));
        assertEquals(String.valueOf(FormValidation.error("project.json path is mandatory")), String.valueOf(descriptor.doCheckProjectJsonPath("")));
        assertEquals(String.valueOf(FormValidation.ok()), String.valueOf(descriptor.doCheckProjectJsonPath(outputPath)));
        assertEquals(String.valueOf(FormValidation.ok()), String.valueOf(descriptor.doCheckProjectJsonPath(projectJsonPath)));
    }
}
