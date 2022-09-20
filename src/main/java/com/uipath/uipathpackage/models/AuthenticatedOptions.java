package com.uipath.uipathpackage.models;
import com.uipath.uipathpackage.util.TraceLevel;

public abstract class AuthenticatedOptions extends LocalizationOptions implements SerializableCliOptions {
    private String username;
    private String password;
    private String accountName;
    private String refreshToken;
    private String authorizationUrl;
    private String accountForApp;
    private String applicationId;
    private String applicationSecret;
    private String applicationScope;
    private String organizationUnit;
    private String orchestratorUrl;
    private String orchestratorTenant;
    private TraceLevel traceLevel;

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountForApp() {
        return accountForApp;
    }

    public void setAccountForApp(String accountForApp) {
        this.accountForApp = accountForApp;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getApplicationSecret() {
        return applicationSecret;
    }

    public void setApplicationSecret(String applicationSecret) {
        this.applicationSecret = applicationSecret;
    }

    public String getApplicationScope() {
        return applicationScope;
    }

    public void setApplicationScope(String applicationScope) {
        this.applicationScope = applicationScope;
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

    public TraceLevel getTraceLevel() {
        return traceLevel;
    }

    public void setTraceLevel(TraceLevel traceLevel) {
        this.traceLevel = traceLevel;
    }

    public String getAuthorizationUrl() {
	    return authorizationUrl;
    }

    public void setAuthorizationUrl(String authorizationUrl) {
	    this.authorizationUrl = authorizationUrl;
    }
    
}
