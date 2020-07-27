package com.uipath.uipathpackage.util;

import com.google.common.collect.ImmutableMap;

public class OutputType {
    public static final ImmutableMap<String, String> outputTypes = ImmutableMap.of(
            "Output type of the project", "None",
            "Pack a process project", "Process",
            "Pack a library project", "Library",
            "Pack a tests project", "Tests",
            "Pack an objects project", "Objects"
    );
}
