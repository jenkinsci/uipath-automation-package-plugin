package com.uipath.uipathpackage.models;

public class AnalyzeOptions extends CommonOptions {
    private String projectPath;
    private String analyzerTraceLevel;
    private boolean stopOnRuleViolation = true;
    private boolean treatWarningsAsErrors = false;
    private String ignoredRules = "";
    private Boolean disableBuiltInNugetFeeds;
    private String governanceFilePath;

    public boolean isDisableBuiltInNugetFeeds() {
        return disableBuiltInNugetFeeds;
    }

    public void setDisableBuiltInNugetFeeds(Boolean disableBuiltInNugetFeeds) {
        this.disableBuiltInNugetFeeds = disableBuiltInNugetFeeds;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public String getAnalyzerTraceLevel() {
        return analyzerTraceLevel;
    }

    public void setAnalyzerTraceLevel(String analyzerTraceLevel) {
        this.analyzerTraceLevel = analyzerTraceLevel;
    }

    public boolean getStopOnRuleViolation() {
        return stopOnRuleViolation;
    }

    public void setStopOnRuleViolation(boolean stopOnRuleViolation) {
        this.stopOnRuleViolation = stopOnRuleViolation;
    }

    public boolean getTreatWarningsAsErrors() {
        return treatWarningsAsErrors;
    }

    public void setTreatWarningsAsErrors(boolean treatWarningsAsErrors) {
        this.treatWarningsAsErrors = treatWarningsAsErrors;
    }

    public String getIgnoredRules() {
        return ignoredRules;
    }

    public void setIgnoredRules(String ignoredRules) {
        this.ignoredRules = ignoredRules;
    }

    public String getGovernanceFilePath() {
        return governanceFilePath;
    }

    public void setGovernanceFilePath(String governanceFilePath) {
        this.governanceFilePath = governanceFilePath;
    }
}
