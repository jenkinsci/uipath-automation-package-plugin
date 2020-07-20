package com.uipath.uipathpackage.util;

import java.util.HashMap;

public class OutputType {
    public static final HashMap<String, String> outputTypes = new HashMap<String, String>(){{
        put("Pack a process project", "Process");
        put("Pack a library project", "Library");
        put("Pack a tests project", "Tests");
        put("Pack an objects project", "Objects");
        put("Output type of the project", "None");
    }};
}
