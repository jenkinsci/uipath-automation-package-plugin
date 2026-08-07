package com.uipath.uipathpackage.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uipath.uipathpackage.actions.AddEnvironmentVariablesAction;
import com.uipath.uipathpackage.util.Utility;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import org.apache.commons.io.FilenameUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.*;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import hudson.Util;

public final class UiPathCliConfiguration {

    private static UiPathCliConfiguration INSTANCE = null;
    public static final String WIN_PLATFORM = "Windows";
    public static final String SELECTED_CLI_VERSION_KEY = "SELECTED_CLI_VERSION_KEY";
    public static final String DEFAULT_CLI_VERSION_KEY = "UiPath.CLI.Version";
    private final String INSTALL_PLATFORM_CONFIGURATION_KEY = "UiPath.CLI.InstallPlatform.Configuration";
    private final String INSTALL_PLATFORM_CONFIGURATION_COUNT_KEY = "UiPath.CLI.InstallPlatform.Configuration.Count";
    private static final String FEED_URL_TEMPLATE = "https://uipath.pkgs.visualstudio.com/Public.Feeds/_packaging/UiPath-Official/nuget/v3/flat2/UiPath.CLI.%s/%s/UiPath.CLI.%s.%s.nupkg";
    private static final Map<String, CliPlatform> PLATFORM_MAP;
    static {
        Map<String, CliPlatform> map = new HashMap<>();
        map.put("Windows.Legacy", CliPlatform.Windows);
        map.put("Windows", CliPlatform.Windows);
        map.put("Linux", CliPlatform.Linux);
        map.put("macOS", CliPlatform.macOS);
        PLATFORM_MAP = Collections.unmodifiableMap(map);
    }
    private final Map<String,Configuration> cliConfigurationMap;
    private final int configurationCount;

    /**
    * This to make this class Singleton , as only want a single configuration to be used through-out our plugin , initialised only once.
    * */
    private UiPathCliConfiguration() throws JsonProcessingException {
        ResourceBundle resourceBundle = ResourceBundle.getBundle("config");
        configurationCount = Integer.parseInt(resourceBundle.getString(INSTALL_PLATFORM_CONFIGURATION_COUNT_KEY));
        cliConfigurationMap = initializeConfigurationMap(resourceBundle);
    }

    public synchronized static UiPathCliConfiguration getInstance() throws JsonProcessingException {
        if (INSTANCE == null) {
            INSTANCE = new UiPathCliConfiguration();
        }
        return INSTANCE;
    }

    public Map<String,Configuration> getConfiguration() {
        Map<String,Configuration> configuration = new HashMap<>();
        cliConfigurationMap.forEach((versionKey,cliConfig)->{
            configuration.put(versionKey,cliConfig.clone());
        });
        return configuration;
    }

    public Configuration getConfiguration(String versionKey) {
        return cliConfigurationMap.getOrDefault(versionKey, null);
    }

    public int getConfigurationCount() {
        return configurationCount;
    }

    public String getDefaultCliVersionKey() {
        return WIN_PLATFORM + "." + ResourceBundle.getBundle("config").getString(DEFAULT_CLI_VERSION_KEY);
    }

    public String getSelectedOrDefaultCliVersionKey(@Nonnull EnvVars envVars) {
        String selectedCliVersionKey = envVars.get(SELECTED_CLI_VERSION_KEY);
        return  Util.fixEmptyAndTrim(selectedCliVersionKey) != null ? selectedCliVersionKey : getDefaultCliVersionKey();
    }

    public void updateSelectedCliVersionKey(@NonNull Run<?, ?> run, @Nonnull String cliVersionKey) throws AbortException {
        Map<String, String> addedEnvVars = Collections.singletonMap(SELECTED_CLI_VERSION_KEY, cliVersionKey);
        AddEnvironmentVariablesAction envAction = run.getAction(AddEnvironmentVariablesAction.class);

        if(envAction == null) {
            envAction = new AddEnvironmentVariablesAction();
            envAction.setAddedEnvironmentVariables(addedEnvVars);
            run.addAction(envAction);
        } else {
            envAction.setAddedEnvironmentVariables(addedEnvVars);
        }
    }

    public FilePath getCliHomeDirectory(@Nonnull Launcher launcher, @Nonnull EnvVars env) throws IOException, InterruptedException {
        FilePath cliHomeDir = new FilePath(launcher.getChannel(), env.expand("${WORKSPACE}")).child("CLI");
        cliHomeDir.mkdirs();
        return cliHomeDir;
    }

    public FilePath getCliRootCachedDirectoryPath(@Nonnull Launcher launcher, @Nonnull EnvVars env, String cliVersionKey) throws IOException, InterruptedException {
        if (!cliConfigurationMap.containsKey(cliVersionKey)) {
            launcher.getListener().getLogger().println("(cacheRootPath) invalid cli configuration might have caused this issue.");
            throw new AbortException("(cacheRootPath) invalid cli configuration might have caused this issue. Version key: " + cliVersionKey);
        }

        Configuration configuration = cliConfigurationMap.get(cliVersionKey);

        FilePath cliHomeDir = getCliHomeDirectory(launcher,env);
        FilePath cachedRootPath = cliHomeDir.child("cached").child(configuration.getName()).child(configuration.getVersion().getComplete());
        cachedRootPath.mkdirs();

        return cachedRootPath;
    }

    public FilePath getCliRootDownloadsDirectoryPath(@Nonnull Launcher launcher, @Nonnull EnvVars env, String cliVersionKey) throws IOException, InterruptedException {
        if(!cliConfigurationMap.containsKey(cliVersionKey)) {
            launcher.getListener().getLogger().println("(downloadsRootPath) invalid cli configuration might have caused this issue.");
            throw new AbortException("(downloadsRootPath) invalid cli configuration might have caused this issue.");
        }

        Configuration configuration = cliConfigurationMap.get(cliVersionKey);

        FilePath cliHomeDir = getCliHomeDirectory(launcher,env);
        FilePath downloadsRootPath = cliHomeDir.child("downloads").child(configuration.getName()).child(configuration.getVersion().getComplete());
        downloadsRootPath.mkdirs();
        launcher.getListener().getLogger().println("Cli root download directory: " + downloadsRootPath.getRemote());

        return downloadsRootPath;
    }

    public Optional<FilePath> getCliPath(@Nonnull Launcher launcher, @Nonnull EnvVars env, String cliVersionKey) {
        PrintStream logger = launcher.getListener().getLogger();
        try {
            FilePath cliCachedPath = getCliRootCachedDirectoryPath(launcher, env, cliVersionKey);
            cliCachedPath = Utility.getDotnetToolCliPath(cliCachedPath);

            if (cliCachedPath.exists()) {
                return Optional.of(cliCachedPath);
            }
        } catch (Exception e) {
            e.printStackTrace(logger);
            logger.println("error while location cached cli path "+e.getMessage());
        }
        return Optional.empty();
    }

    public void AddCliConfiguration(String cliVersionKey, UiPathCliConfiguration.Configuration configuration) {
        if(cliConfigurationMap.containsKey(cliVersionKey)) return;
        cliConfigurationMap.put(cliVersionKey, configuration);
    }

    private Map<String,Configuration> initializeConfigurationMap(@Nonnull ResourceBundle resourceBundle) throws JsonProcessingException {

        String json = resourceBundle.getString(INSTALL_PLATFORM_CONFIGURATION_KEY);
        ObjectMapper mapper = new ObjectMapper();
        String[] displayNames = mapper.readValue(json, new TypeReference<String[]>() {
        });
        Map<String,Configuration> cliConfigurationMap = new HashMap<>();
        for (String displayName : displayNames) {
            if ("CustomVersion".equals(displayName)) {
                // Create a special Configuration for CustomVersion with only name populated
                Configuration.Version dummyVersion = new Configuration.Version(0, 0, 0, "CustomVersion");
                Configuration config = new Configuration(displayName, displayName, dummyVersion, false, false, "", "", CliPlatform.Unknown);
                cliConfigurationMap.put(config.getConfigurationUniqueId(), config);
                continue;
            }
            String fullPackageName = "UiPath.CLI." + displayName;
            try {
                Configuration configuration = getConfigurationFromPackageName(fullPackageName);
                cliConfigurationMap.put(configuration.getConfigurationUniqueId(), configuration);
            } catch (Exception e) {
                throw new JsonProcessingException("Failed to parse configuration for " + displayName, e) {};
            }
        }
        return cliConfigurationMap;
    }

    public Configuration getConfigurationFromPackageName(String cliPackageName) throws Exception {
        // Extract package name without extension
        String packageName;
        if (cliPackageName.endsWith(".nupkg")) {
            String fileName = FilenameUtils.getBaseName(cliPackageName);
            packageName = fileName.substring(0, fileName.lastIndexOf('.'));
        } else {
            packageName = cliPackageName;
        }

        // Extract platform and version from CLI package name
        // Pattern: UiPath.CLI.{Platform}.{Version}
        // Platform can be: Windows, Windows.Legacy, Linux, macOS
        // Version can be: 25.10.0-20251017-19 or 25.10.9424.14050
        Pattern packageRegex = Pattern.compile("^UiPath\\.CLI\\.(Windows\\.Legacy|Windows|Linux|macOS)\\.((?:\\d+\\.\\d+\\.\\d+|\\d+\\.\\d+\\.\\d+\\.\\d+|\\d+\\.\\d+\\.\\d+\\.\\d+\\.\\d+)(?:-[A-Za-z0-9-]+|))$");
        Matcher match = packageRegex.matcher(packageName);

        if (!match.matches()) {
            throw new Exception("Invalid CLI package name format: " + cliPackageName + " Extracted package name: " + packageName);
        }

        String platformString = match.group(1); // Windows.Legacy, Windows, Linux, or Mac
        String rawVersion = match.group(2); // e.g., 25.10.0-20251017-19 or 25.10.9424.14050

        // Map platform string to CliPlatform enum
        CliPlatform platform = PLATFORM_MAP.getOrDefault(platformString, CliPlatform.Unknown);

        // Normalize version to proper semver format
        // Convert 25.10.9424.14050 -> 25.10.9424-14050
        // Keep 25.10.0-20251017-19 -> 25.10.0-20251017-19 (already semver)
        String fullVersion = rawVersion;
        Pattern legacyVersionPattern = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)$");
        Matcher legacyMatch = legacyVersionPattern.matcher(rawVersion);
        if (legacyMatch.matches()) {
            fullVersion = legacyMatch.group(1) + "." + legacyMatch.group(2) + "." + legacyMatch.group(3) + "-" + legacyMatch.group(4);
        }

        // Determine compatibility
        boolean windowsCompatible = "Windows".equals(platformString) || "Windows.Legacy".equals(platformString);
        boolean linuxCompatible = "Linux".equals(platformString);

        // Parse version components
        Pattern versionRegex = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)");
        Matcher versionMatch = versionRegex.matcher(fullVersion);
        if (!versionMatch.find()) {
            throw new Exception("Invalid version format: " + fullVersion);
        }

        int major = Integer.parseInt(versionMatch.group(1));
        int minor = Integer.parseInt(versionMatch.group(2));
        int patch = Integer.parseInt(versionMatch.group(3));

        // Build feedUrl using original platform string to preserve Windows.Legacy
        String feedUrl = String.format(FEED_URL_TEMPLATE, platformString, rawVersion, platformString, rawVersion);

        // Construct Configuration
        Configuration.Version version = new Configuration.Version(major, minor, patch, rawVersion);
        String name = "UiPath.CLI." + platformString;
        String displayName = platformString + "." + rawVersion;
        Configuration config = new Configuration(
            displayName,
            name,
            version,
            windowsCompatible,
            linuxCompatible,
            "", // description
            feedUrl,
            platform
        );
        return config;
    }

    public enum CliPlatform {
        Unknown,
        Windows,
        Linux,
        macOS
    }

    public static final class Configuration implements Cloneable , Serializable {
        private String displayName;
        private String name;
        private Version version;
        private boolean windowsCompatible;
        private boolean linuxCompatible;
        private String description;
        private String feedUrl;
        private CliPlatform platform;

        public Configuration() {
        }

        public Configuration(String displayName, String name, Version version, boolean windowsCompatible, boolean linuxCompatible, String description, String feedUrl, CliPlatform platform) {
            this.displayName = displayName;
            this.name = name;
            this.version = version;
            this.windowsCompatible = windowsCompatible;
            this.linuxCompatible = linuxCompatible;
            this.description = description;
            this.feedUrl = feedUrl;
            this.platform = platform;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getName() {
            return name;
        }

        public Version getVersion() {
            return version;
        }

        public boolean getWindowsCompatible() {
            return windowsCompatible;
        }

        public boolean getLinuxCompatible() {
            return linuxCompatible;
        }

        public String getDescription() {
            return description;
        }

        public String getFeedUrl() {
            return feedUrl;
        }

        public CliPlatform getPlatform() {
            return platform;
        }

        public String getConfigurationUniqueId() {
            return this.getDisplayName();
        }

        @Override
        protected Configuration clone() {
            Version version = new Version(this.version.major, this.version.minor, this.version.patch, this.version.complete);
            Configuration configuration = new Configuration(this.displayName, this.name, version, this.windowsCompatible, this.linuxCompatible, this.description, this.feedUrl, this.platform);
            return configuration;
        }

        public static final class Version implements Cloneable,Serializable {

            private int major;
            private int minor;
            private int patch;
            private String complete;

            public Version(int major, int minor, int patch, String complete) {
                this.major = major;
                this.minor = minor;
                this.patch = patch;
                this.complete = complete;
            }

            public Version() {
            }

            public int getMajor() {
                return major;
            }

            public int getMinor() {
                return minor;
            }

            public int getPatch() {
                return patch;
            }

            public String getComplete() {
                return complete;
            }

            @Override
            protected Version clone() {
                Version version = new Version(this.major, this.minor, this.patch, this.complete);
                return version;
            }
        }

    }
}
