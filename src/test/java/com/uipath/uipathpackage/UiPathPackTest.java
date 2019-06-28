package com.uipath.uipathpackage;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.FormValidation;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.isA;

public class UiPathPackTest {

    private static UiPathPack.AutoEntry autoEntry = null;
    private static UiPathPack.ManualEntry manualEntry = null;
    private static String parentProjectPath = null;
    private static String projectJsonPath = null;
    private static String projectPath = null;
    private static String outputPath = null;
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
        File projectJson = new File(Objects.requireNonNull(classLoader.getResource("TestProject/project.json")).getPath());
        projectJsonPath = projectJson.getAbsolutePath();
        outputPath = projectPath;
        autoEntry = new UiPathPack.AutoEntry();
        manualEntry = new UiPathPack.ManualEntry("1.2.3.${BUILD_NUMBER}");
    }

    @Test
    public void testAutoVersionConfigRoundtrip() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new UiPathPack(autoEntry, projectJsonPath, outputPath));
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new UiPathPack(autoEntry, projectJsonPath, outputPath), project.getBuildersList().get(0));
    }

    @Test
    public void testManualVersionConfigRoundtrip() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new UiPathPack(manualEntry, projectJsonPath, outputPath));
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new UiPathPack(manualEntry, projectJsonPath, outputPath), project.getBuildersList().get(0));
    }

    @Test
    public void testBuildWithJson() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        UiPathPack builder = new UiPathPack(autoEntry, projectJsonPath, outputPath);
        project.getBuildersList().add(builder);
        doNothing().when(util).validateParams(isA(String.class), isA(String.class));
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Running pack with the arguments: -pack", build);
        jenkins.assertLogContains("Finished: SUCCESS", build);
    }

    @Test
    public void testBuildWithParentProject() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        UiPathPack builder = new UiPathPack(autoEntry, parentProjectPath, outputPath);
        doNothing().when(util).validateParams(isA(String.class), isA(String.class));
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Running pack with the arguments: -pack", build);
        jenkins.assertLogContains("Finished: SUCCESS", build);
    }

    @Test
    public void testBuildWithProject() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        UiPathPack builder = new UiPathPack(autoEntry, projectPath, outputPath);
        project.getBuildersList().add(builder);
        doNothing().when(util).validateParams(isA(String.class), isA(String.class));
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Running pack with the arguments: -pack", build);
        jenkins.assertLogContains("Finished: SUCCESS", build);
    }

    @Test
    public void testBuildWithEnvVar() throws Exception {
        outputPath = "${WORKSPACE}";
        FreeStyleProject project = jenkins.createFreeStyleProject();
        UiPathPack builder = new UiPathPack(autoEntry, projectPath, outputPath);
        project.getBuildersList().add(builder);
        doNothing().when(util).validateParams(isA(String.class), isA(String.class));
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Running pack with the arguments: -pack", build);
        jenkins.assertLogContains("Finished: SUCCESS", build);
    }

    @Test
    public void testBuildWithEnvVarManualEntry() throws Exception {
        outputPath = "${WORKSPACE}";
        FreeStyleProject project = jenkins.createFreeStyleProject();
        UiPathPack builder = new UiPathPack(manualEntry, projectPath, outputPath);
        project.getBuildersList().add(builder);
        doNothing().when(util).validateParams(isA(String.class), isA(String.class));
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Running pack with the arguments: -pack", build);
        jenkins.assertLogContains("Finished: SUCCESS", build);
    }

    @Test
    public void testUiPathPackClassAutoEntry() {
        UiPathPack uipathPack = new UiPathPack(autoEntry, projectPath, outputPath);
        assertEquals(autoEntry, uipathPack.getVersion());
        assertEquals(projectPath, uipathPack.getProjectJsonPath());
        assertEquals(outputPath, uipathPack.getOutputPath());
    }

    @Test
    public void testUiPathPackClassManualEntry() {
        UiPathPack uipathPack = new UiPathPack(manualEntry, projectPath, outputPath);
        assertEquals(manualEntry, uipathPack.getVersion());
    }

    @Test
    public void testUiPathPackClassDescriptor() {
        UiPathPack.DescriptorImpl descriptor = new UiPathPack.DescriptorImpl();
        assertEquals(String.valueOf(FormValidation.error("Output Path is mandatory")), String.valueOf(descriptor.doCheckOutputPath("")));
        assertEquals(String.valueOf(FormValidation.error("Project Json Path is mandatory")), String.valueOf(descriptor.doCheckProjectJsonPath("")));
        assertEquals(String.valueOf(FormValidation.ok()), String.valueOf(descriptor.doCheckProjectJsonPath(outputPath)));
        assertEquals(String.valueOf(FormValidation.ok()), String.valueOf(descriptor.doCheckProjectJsonPath(projectJsonPath)));
    }
}
