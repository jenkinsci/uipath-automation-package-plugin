package com.uipath.uipathpackage.models;

public abstract class LocalizationOptions extends TelemetryOptions {
    private String language;

    public String getLanguage() { return language; }

    public void setLanguage(String language) { this.language = language; }
}
