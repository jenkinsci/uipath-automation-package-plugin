package com.uipath.uipathpackage.models;

import com.uipath.uipathpackage.util.CliGetFlow;
import hudson.PluginWrapper;
import jenkins.model.Jenkins;
import org.jenkinsci.main.modules.instance_identity.InstanceIdentity;

public abstract class TelemetryOptions {
    private final String telemetryOrigin;

    private String pipelineCorrelationId;
    private String extensionClientOrganizationId;
    private String telemetryOriginVersion;
    private String cliGetFlow;

    public TelemetryOptions() {
        this.telemetryOrigin = "Jenkins";
    }

    public void populateAdditionalTelemetryData() {
        this.telemetryOriginVersion = getPluginVersion();
        this.extensionClientOrganizationId = getInstanceIdentity();
    }

    private String getInstanceIdentity() {
        return  InstanceIdentity.get().getEncodedPublicKey();
    }

    public String getTelemetryOriginVersion() {
        return telemetryOriginVersion;
    }

    private String getPluginVersion() {
        Jenkins jenkinsInstance = Jenkins.get();
        PluginWrapper pluginWrapper = jenkinsInstance.getPluginManager().getPlugin("uipath-automation-package");

        if (pluginWrapper != null)
            return pluginWrapper.getVersion();

        return "";
    }

    public String getPipelineCorrelationId() {
        return pipelineCorrelationId;
    }

    public void setPipelineCorrelationId(String buildTag) {
        this.pipelineCorrelationId = getInstanceIdentity() + buildTag;
    }

    public String getExtensionClientOrganizationId() {
        return extensionClientOrganizationId;
    }

    public String getTelemetryOrigin() {
        return telemetryOrigin;
    }

    public String getCliGetFlow() {
        return cliGetFlow;
    }

    public void setCliGetFlow(CliGetFlow cliGetFlow) {
        this.cliGetFlow = cliGetFlow.toString();
    }
}
