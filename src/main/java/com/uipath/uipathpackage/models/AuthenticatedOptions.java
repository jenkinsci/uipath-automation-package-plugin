package com.uipath.uipathpackage.models;
import com.uipath.uipathpackage.util.TraceLevel;

public interface AuthenticatedOptions {
    public String getAccountName();
    public void setAccountName(String accountName);
    public String getAccountForApp();
    public void setAccountForApp(String accountForApp);
    public String getApplicationId();
    public void setApplicationId(String applicationId);
    public String getApplicationSecret();
    public void setApplicationSecret(String applicationSecret);
    public String getApplicationScope();
    public void setApplicationScope(String applicationScope);
    public String getUsername();
    public void setUsername(String username);
    public String getPassword();
    public void setPassword(String password);
    public String getRefreshToken();
    public void setRefreshToken(String refreshToken);
    public String getOrchestratorUrl();
    public void setOrchestratorUrl(String orchestratorUrl);
    public String getOrchestratorTenant();
    public void setOrchestratorTenant(String orchestratorTenant);
    public String getOrganizationUnit();
    public void setOrganizationUnit(String organizationUnit);
    public TraceLevel getTraceLevel();
    public void setTraceLevel(TraceLevel traceLevel);
    public String getAuthorizationUrl();
    public void setAuthorizationUrl(String authorizationUrl);
}
