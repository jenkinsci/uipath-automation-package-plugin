package com.uipath.uipathpackage;

import com.uipath.uipathpackage.configuration.UiPathCliConfiguration;
import com.uipath.uipathpackage.util.TraceLevel;
import com.uipath.uipathpackage.util.Utility;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Answer1;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class UiPathInstallPlatformTests {

    private static FilePath pathToRootDownload = null;
    @Rule
    public final JenkinsRule jenkins = new JenkinsRule();
    private FreeStyleProject project;
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock
    Utility utility;

    public UiPathInstallPlatformTests() {

    }

    @Before
    public void beforeTest() throws IOException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        pathToRootDownload = new FilePath(new File(Objects.requireNonNull(classLoader.getResource("CliNupkg")).getPath()));
    }

    @Before
    public void setUp() throws IOException {
        project = jenkins.createFreeStyleProject("freeStyleProject1");
    }

    @Test
    public void testInstallPlatformTaskCachesPrepackagedCli() throws Exception {
        UiPathInstallPlatform installPlatform = new UiPathInstallPlatform("", TraceLevel.Error);
        project.getBuildersList().add(installPlatform);
        project = jenkins.configRoundtrip(project);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("(caching) extracting the pre-packaged cli...", build);
    }

    public void testInstallPlatformTaskCachesSpecifiedCliAfterDownloading() throws Exception {
        UiPathCliConfiguration configuration = mock(UiPathCliConfiguration.class);
        doAnswer(new Answer() {
            public Void answer(InvocationOnMock invocation) {
                return null;
            }
        }).when(utility).downloadCli(anyString(),any(FilePath.class),any(TaskListener.class));
        when(configuration.getConfiguration()).thenCallRealMethod();
        when(configuration.getCliRootDownloadsDirectoryPath(any(Launcher.class),any(EnvVars.class),anyString())).thenReturn(pathToRootDownload);

        UiPathInstallPlatform installPlatform = new UiPathInstallPlatform("", TraceLevel.Error);
        installPlatform.setCliVersion("WIN_22.10.8438.32859");
        project.getBuildersList().add(installPlatform);
        UiPathInstallPlatform installPlatform2 = new UiPathInstallPlatform("",TraceLevel.Error);
        installPlatform2.setCliVersion("WIN_22.10.8438.32859");
        project.getBuildersList().add(installPlatform2);
        project = jenkins.configRoundtrip(project);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("(caching) extracting the downloaded cli...", build);
        jenkins.assertLogContains("Downloading", build);
        jenkins.assertLogContains("Downloaded", build);
        jenkins.assertLogContains("cli is already cached..",build);
    }

    public void testInstallPlatformTaskCachesSpecifiedCliFromProvidedLocation() throws Exception {
        //when(envVars.expand("${WORKSPACE}")).thenReturn(tmpDir.getRemote());
        UiPathInstallPlatform installPlatform = new UiPathInstallPlatform("", TraceLevel.Error);
        installPlatform.setCliVersion("WIN_22.10.8438.32859");
        project.getBuildersList().add(installPlatform);
        project = jenkins.configRoundtrip(project);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("(caching) extracting the pre-packaged cli...", build);
    }

    public void testInstallPlatformTaskReCachesCliIfForceInstallIsSelected() throws Exception {
        //when(envVars.expand("${WORKSPACE}")).thenReturn(tmpDir.getRemote());
        String cliVersion = "WIN_22.10.8438.32859";
        UiPathInstallPlatform installPlatform = new UiPathInstallPlatform("", TraceLevel.Error);
        installPlatform.setCliVersion(cliVersion);
        project.getBuildersList().add(installPlatform);
        UiPathInstallPlatform installPlatform2 = new UiPathInstallPlatform("",TraceLevel.Error);
        installPlatform.setForceInstall(true);
        installPlatform2.setCliVersion(cliVersion);
        project.getBuildersList().add(installPlatform2);
        project = jenkins.configRoundtrip(project);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("(caching) extracting the downloaded cli...", build);
        jenkins.assertLogContains("Downloading", build);
        jenkins.assertLogContains("Downloaded", build);
        jenkins.assertLogContains("force installing the cli , any previous cache for version "+cliVersion+" will be invalidate..", build);
        jenkins.assertLogContains("(caching) extracting the downloaded cli...", build);
    }

}
