package com.uipath.uipathpackage.models;

public abstract class AuthenticatedOptions implements SerializableCliOptions {
    private String username;
    private String password;
    private String refreshToken;
    private String accountName;
    private String organizationUnit;
    private String orchestratorUrl;
    private String orchestratorTenant;

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getOrchestratorUrl() {
        return orchestratorUrl;
    }

    public void setOrchestratorUrl(String orchestratorUrl) {
        this.orchestratorUrl = orchestratorUrl;
    }

    public String getOrchestratorTenant() {
        return orchestratorTenant;
    }

    public void setOrchestratorTenant(String orchestratorTenant) {
        this.orchestratorTenant = orchestratorTenant;
    }

    public String getOrganizationUnit() {
        return organizationUnit;
    }

    public void setOrganizationUnit(String organizationUnit) {
        this.organizationUnit = organizationUnit;
    }
}
