package com.uipath.uipathpackage;

import com.uipath.uipathpackage.util.Utility;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.TaskListener;
import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class UtilityTest {
    private FilePath tmpDir;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock
    TaskListener listener;
    @Mock
    PrintStream logger;
    @Mock
    EnvVars envVars;

    @Before
    public void beforeTest() throws IOException {
        File testDir = new File(System.getProperty("java.io.tmpdir"), "UiPathTest");
        if (!testDir.exists()) {
            if (!testDir.mkdir()) throw new IOException("Failed to create test directory");
        }
        FileUtils.cleanDirectory(testDir);
        this.tmpDir = new FilePath(testDir);
    }

    @Test
    public void testCliIsCorrectlyExtracted() throws IOException, InterruptedException, URISyntaxException {
        when(listener.getLogger()).thenReturn(logger);
        doNothing().when(logger).println(isA(String.class));
        File resource = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("").toURI()).getPath());
        File jarFile = new File(resource, "../uipath-automation-package.jar");
        when(envVars.expand(isA(String.class))).thenReturn(jarFile.getAbsolutePath());
        Utility util = new Utility();
        FilePath cliPath = util.extractCliApp(tmpDir, listener,  envVars);
        assertThat(cliPath.getRemote(), CoreMatchers.containsString("cli.exe"));
        assertEquals(true, cliPath.exists());
    }

    @Test(expected = InvalidParameterException.class)
    public void testValidateParams(){
        Utility util = new Utility();
        util.validateParams("", "testException");
    }
}
