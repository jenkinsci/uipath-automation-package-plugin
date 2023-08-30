package com.uipath.uipathpackage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.uipath.uipathpackage.configuration.UiPathCliConfiguration;
import com.uipath.uipathpackage.util.Utility;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class UiPathCliConfigurationTests {

    private FilePath tmpDir;
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock
    TaskListener listener;
    @Mock
    PrintStream logger;
    @Mock
    EnvVars envVars;
    @Mock
    Launcher launcher;
    @Rule
    public final JenkinsRule jenkins = new JenkinsRule();
    private FreeStyleProject project;

    @Before
    public void beforeTest() throws IOException {
        File testDir = new File(System.getProperty("java.io.tmpdir"), "UiPathTest");
        if (!testDir.exists()) {
            if (!testDir.mkdir()) throw new IOException("Failed to create test directory");
        }
        FileUtils.cleanDirectory(testDir);
        this.tmpDir = new FilePath(testDir);

        project = jenkins.createFreeStyleProject("freeStyleProject1");
    }

    @Test
    public void testCliConfigurationIsProperlyInitialized() throws JsonProcessingException {
        UiPathCliConfiguration cliConfiguration = UiPathCliConfiguration.getInstance();
        Map<String, UiPathCliConfiguration.Configuration> configurationMap =  cliConfiguration.getConfiguration();
        Assert.notNull(configurationMap);
        Assert.notEmpty(configurationMap);
        assertEquals(configurationMap.size(), cliConfiguration.getConfigurationCount());
    }

    @Test
    public void testCliConfigurationGetCliPathSucceedsForDefaultCli() throws IOException, URISyntaxException, InterruptedException {
        when(launcher.getListener()).thenReturn(listener);
        when(listener.getLogger()).thenReturn(logger);
        doNothing().when(logger).println(isA(String.class));
        File resource = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("")).getPath());
        File jarFile = new File(resource, "../uipath-automation-package.jar");
        FilePath workspacePath = tmpDir;
        when(envVars.expand("${JENKINS_HOME}\\plugins\\uipath-automation-package\\WEB-INF\\lib\\uipath-automation-package.jar")).thenReturn(jarFile.getAbsolutePath());
        when(envVars.expand("${WORKSPACE}")).thenReturn(workspacePath.getRemote());
        Utility util = new Utility();
        UiPathCliConfiguration cliConfiguration = UiPathCliConfiguration.getInstance();
        util.extractCliApp(cliConfiguration.getCliRootCachedDirectoryPath(launcher, envVars, cliConfiguration.getDefaultCliVersionKey()), listener,  envVars);
        FilePath cliPath = cliConfiguration.getCliPath(launcher,envVars, cliConfiguration.getDefaultCliVersionKey()).get();
        assertThat(cliPath.getRemote(), CoreMatchers.containsString("cli.dll"));
        assertEquals(true, cliPath.exists());
    }

    @Test
    public void testCliConfigurationGetCliPathFailsForUnknownCliVersion() throws Exception {
        when(launcher.getListener()).thenReturn(listener);
        when(listener.getLogger()).thenReturn(logger);
        when(envVars.expand("${WORKSPACE}")).thenReturn(tmpDir.getRemote());
        UiPathCliConfiguration cliConfiguration = UiPathCliConfiguration.getInstance();
        try {
            project = jenkins.configRoundtrip(project);
            FreeStyleBuild build = project.scheduleBuild2(0).get();
            cliConfiguration.updateSelectedCliVersionKey(build, "WIN_22.10.1234.1223");
        } catch (AbortException e) {
            assertTrue(true);
        }
        try {
            FilePath cliPath = cliConfiguration.getCliPath(launcher, envVars, cliConfiguration.getDefaultCliVersionKey()).get();
            assertEquals(true, !cliPath.exists());
        } catch (NoSuchElementException e) {
            assertTrue(true);
        }
    }

}
