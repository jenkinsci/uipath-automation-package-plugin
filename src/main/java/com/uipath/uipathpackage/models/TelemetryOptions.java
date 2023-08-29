package com.uipath.uipathpackage.models;
import com.uipath.uipathpackage.util.CliGetFlow;

public interface TelemetryOptions {
    public void populateAdditionalTelemetryData();
    public String getTelemetryOriginVersion();
    public String getPipelineCorrelationId();

    public void setPipelineCorrelationId(String buildTag);

    public String getExtensionClientOrganizationId();

    public String getTelemetryOrigin();

    public String getCliGetFlow();

    public void setCliGetFlow(CliGetFlow cliGetFlow);
}
