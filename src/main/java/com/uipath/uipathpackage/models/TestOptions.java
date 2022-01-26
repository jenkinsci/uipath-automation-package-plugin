package com.uipath.uipathpackage.models;

public class TestOptions extends AuthenticatedOptions {
    private String projectPath;
    private String testSet;
    private String environment;
    private String testReportType;
    private String testReportDestination;
    private String parametersFilePath;
    private boolean attachRobotLogs;
    private Integer timeout;

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getTestSet() {
        return testSet;
    }

    public void setTestSet(String testSet) {
        this.testSet = testSet;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public String getTestReportType() {
        return testReportType;
    }

    public void setTestReportType(String testReportType) {
        this.testReportType = testReportType;
    }

    public String getTestReportDestination() {
        return testReportDestination;
    }

    public void setTestReportDestination(String testReportDestination) {
        this.testReportDestination = testReportDestination;
    }

    public String getParametersFilePath() {
        return parametersFilePath;
    }

    public void setParametersFilePath(String parametersFilePath) {
        this.parametersFilePath = parametersFilePath;
    }

	public boolean getAttachRobotLogs() {
		return attachRobotLogs;
	}

	public void setAttachRobotLogs(boolean attachRobotLogs) {
		this.attachRobotLogs = attachRobotLogs;
	}

}
