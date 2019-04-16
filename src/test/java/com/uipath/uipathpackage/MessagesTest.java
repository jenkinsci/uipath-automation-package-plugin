package com.uipath.uipathpackage;

import org.junit.Test;
import org.jvnet.localizer.Localizable;
import org.jvnet.localizer.ResourceBundleHolder;

import static org.junit.Assert.assertEquals;

public class MessagesTest {

    private final static ResourceBundleHolder holder = ResourceBundleHolder.get(Messages.class);

    @Test
    public void testUiPathDeploy_DescriptorImpl_DisplayName() {
        assertEquals("UiPath Deploy", Messages.UiPathDeploy_DescriptorImpl_DisplayName());
    }

    @Test
    public void testUiPathDeploy_DescriptorImpl_errors_missingCredentialsId() {
        assertEquals("Cannot find the selected credentials", Messages.UiPathDeploy_DescriptorImpl_errors_missingCredentialsId());
    }

    @Test
    public void testUiPathDeploy_DescriptorImpl_errors_missingOrchestratorAddress() {
        assertEquals("Orchestrator Address is mandatory", Messages.UiPathDeploy_DescriptorImpl_errors_missingOrchestratorAddress());
    }

    @Test
    public void testUiPathDeploy_DescriptorImpl_errors_missingPackagePath() {
        assertEquals("Package Path is mandatory", Messages.UiPathDeploy_DescriptorImpl_errors_missingPackagePath());
    }

    @Test
    public void testUiPathPack_DescriptorImpl_DisplayName() {
        assertEquals("UiPath Pack", Messages.UiPathPack_DescriptorImpl_DisplayName());
    }

    @Test
    public void testUiPathPack_DescriptorImpl_error_missingOutputPath() {
        assertEquals("Output Path is mandatory", Messages.UiPathPack_DescriptorImpl_error_missingOutputPath());
    }

    @Test
    public void testUiPathPack_DescriptorImpl_errors_missingProjectJsonPath() {
        assertEquals("Project Json Path is mandatory", Messages.UiPathPack_DescriptorImpl_errors_missingProjectJsonPath());
    }

    @Test
    public void testUiPathPack_ManualEntry_DescriptorImpl_DisplayName() {
        assertEquals("Use custom package versioning", Messages.UiPathPack_ManualEntry_DescriptorImpl_DisplayName());
    }

    @Test
    public void testUiPathPack_AutoEntry_DescriptorImpl_DisplayName() {
        assertEquals("Auto generate the package version", Messages.UiPathPack_AutoEntry_DescriptorImpl_DisplayName());
    }

    @Test
    public void test_UiPathDeploy_DescriptorImpl_DisplayName() {
        assertEquals(String.valueOf(new Localizable(holder, "UiPathDeploy.DescriptorImpl.DisplayName")), String.valueOf(Messages._UiPathDeploy_DescriptorImpl_DisplayName()));
    }

    @Test
    public void test_UiPathDeploy_DescriptorImpl_errors_missingCredentialsId() {
        assertEquals(String.valueOf(new Localizable(holder, "UiPathDeploy.DescriptorImpl.errors.missingCredentialsId")), String.valueOf(Messages._UiPathDeploy_DescriptorImpl_errors_missingCredentialsId()));
    }

    @Test
    public void test_UiPathDeploy_DescriptorImpl_errors_missingOrchestratorAddress() {
        assertEquals(String.valueOf(new Localizable(holder, "UiPathDeploy.DescriptorImpl.errors.missingOrchestratorAddress")), String.valueOf(Messages._UiPathDeploy_DescriptorImpl_errors_missingOrchestratorAddress()));
    }

    @Test
    public void test_UiPathDeploy_DescriptorImpl_errors_missingPackagePath() {
        assertEquals(String.valueOf(new Localizable(holder, "UiPathDeploy.DescriptorImpl.errors.missingPackagePath")), String.valueOf(Messages._UiPathDeploy_DescriptorImpl_errors_missingPackagePath()));
    }

    @Test
    public void test_UiPathPack_DescriptorImpl_DisplayName() {
        assertEquals(String.valueOf(new Localizable(holder, "UiPathPack.DescriptorImpl.DisplayName")), String.valueOf(Messages._UiPathPack_DescriptorImpl_DisplayName()));
    }

    @Test
    public void test_UiPathPack_DescriptorImpl_error_missingOutputPath() {
        assertEquals(String.valueOf(new Localizable(holder, "UiPathPack.DescriptorImpl.error.missingOutputPath")), String.valueOf(Messages._UiPathPack_DescriptorImpl_error_missingOutputPath()));
    }

    @Test
    public void test_UiPathPack_DescriptorImpl_errors_missingProjectJsonPath() {
        assertEquals(String.valueOf(new Localizable(holder, "UiPathPack.DescriptorImpl.errors.missingProjectJsonPath")), String.valueOf(Messages._UiPathPack_DescriptorImpl_errors_missingProjectJsonPath()));
    }

    @Test
    public void test_UiPathPack_ManualEntry_DescriptorImpl_DisplayName() {
        assertEquals(String.valueOf(new Localizable(holder, "UiPathPack.ManualEntry.DescriptorImpl.DisplayName")), String.valueOf(Messages._UiPathPack_ManualEntry_DescriptorImpl_DisplayName()));
    }

    @Test
    public void test_UiPathPack_AutoEntry_DescriptorImpl_DisplayName() {
        assertEquals(String.valueOf(new Localizable(holder, "UiPathPack.AutoEntry.DescriptorImpl.DisplayName")), String.valueOf(Messages._UiPathPack_AutoEntry_DescriptorImpl_DisplayName()));
    }
}
