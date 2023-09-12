package com.uipath.uipathpackage.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uipath.uipathpackage.actions.AddEnvironmentVariablesAction;
import com.uipath.uipathpackage.util.EnvironmentVariablesConsts;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.*;

public final class UiPathCliConfiguration {

    private static UiPathCliConfiguration INSTANCE = null;
    public static final String WIN_PLATFORM = "WIN";
    public static final String X_PLATFORM = "X";
    public static final String SELECTED_CLI_VERSION_KEY = "SELECTED_CLI_VERSION_KEY";
    public static final String DEFAULT_CLI_VERSION_KEY = "UiPath.CLI.Version";
    private final String INSTALL_PLATFORM_CONFIGURATION_KEY = "UiPath.CLI.InstallPlatform.Configuration";
    private final String INSTALL_PLATFORM_CONFIGURATION_COUNT_KEY = "UiPath.CLI.InstallPlatform.Configuration.Count";
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

    public int getConfigurationCount() {
        return configurationCount;
    }

    public String getDefaultCliVersionKey() {
        return WIN_PLATFORM + "_" + ResourceBundle.getBundle("config").getString(DEFAULT_CLI_VERSION_KEY);
    }

    public String getSelectedOrDefaultCliVersionKey(@Nonnull EnvVars envVars) {
        String selectedCliVersionKey = envVars.get(SELECTED_CLI_VERSION_KEY);
        return  StringUtils.isNotBlank(selectedCliVersionKey) ? selectedCliVersionKey : getDefaultCliVersionKey();
    }

    public void updateSelectedCliVersionKey(@NonNull Run<?, ?> run, @Nonnull String cliVersionKey) throws AbortException {
        if(!cliConfigurationMap.containsKey(cliVersionKey)) {
            throw new AbortException("(cacheRootPath) invalid cli configuration might have caused this issue.");
        }
        Map<String, String> addedEnvVars = Collections.singletonMap(SELECTED_CLI_VERSION_KEY, cliVersionKey);
        AddEnvironmentVariablesAction envAction = new AddEnvironmentVariablesAction(addedEnvVars);
        run.addAction(envAction);
    }

    public FilePath getCliHomeDirectory(@Nonnull Launcher launcher, @Nonnull EnvVars env) throws IOException, InterruptedException {
        FilePath cliHomeDir = new FilePath(launcher.getChannel(), env.expand("${WORKSPACE}")).child("CLI");
        cliHomeDir.mkdirs();
        return cliHomeDir;
    }

    public FilePath getCliRootCachedDirectoryPath(@Nonnull Launcher launcher, @Nonnull EnvVars env, String cliVersionKey) throws IOException, InterruptedException {
        if(!cliConfigurationMap.containsKey(cliVersionKey)) {
            launcher.getListener().getLogger().println("(cacheRootPath) invalid cli configuration might have caused this issue.");
            throw new AbortException("(cacheRootPath) invalid cli configuration might have caused this issue.");
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
        return downloadsRootPath;
    }

    public Optional<FilePath> getCliPath(@Nonnull Launcher launcher, @Nonnull EnvVars env, String cliVersionKey) {
        PrintStream logger = launcher.getListener().getLogger();
        try {
            FilePath cliCachedPath = getCliRootCachedDirectoryPath(launcher, env, cliVersionKey);
            Configuration configuration = cliConfigurationMap.get(cliVersionKey);
            if (configuration.getVersion().getMajor() >= 22) {
                cliCachedPath = cliCachedPath.child("tools").child("uipcli.dll");
            } else {
                /** To Support Backward compatibility cli-21.10.xxx.xxx conventions needs to be followed.*/
                cliCachedPath = cliCachedPath.child("lib").child("net461").child("uipcli.exe");
            }
            if (cliCachedPath.exists()) {
                return Optional.of(cliCachedPath);
            }
        } catch (Exception e) {
            e.printStackTrace(logger);
            logger.println("error while location cached cli path "+e.getMessage());
        }
        return Optional.empty();
    }

    private Map<String,Configuration> initializeConfigurationMap(@Nonnull ResourceBundle resourceBundle) throws JsonProcessingException {

        String json = resourceBundle.getString(INSTALL_PLATFORM_CONFIGURATION_KEY);
        ObjectMapper mapper = new ObjectMapper();
        Configuration[] configurations = mapper.readValue(json, new TypeReference<Configuration[]>() {
        });
        Map<String,Configuration> cliConfigurationMap = new HashMap<>();
        for (Configuration configuration :
                configurations) {
            cliConfigurationMap.put(configuration.getConfigurationUniqueId(),configuration);
        }
        return cliConfigurationMap;
    }

    public static final class Configuration implements Cloneable , Serializable {
        private String displayName;
        private String name;
        private Version version;
        private boolean windowsCompatible;
        private boolean linuxCompatible;
        private String description;
        private String feedUrl;

        public Configuration() {
        }

        public Configuration(String displayName, String name, Version version, boolean windowsCompatible, boolean linuxCompatible, String description, String feedUrl) {
            this.displayName = displayName;
            this.name = name;
            this.version = version;
            this.windowsCompatible = windowsCompatible;
            this.linuxCompatible = linuxCompatible;
            this.description = description;
            this.feedUrl = feedUrl;
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

        public String getConfigurationUniqueId() {
            return (this.getWindowsCompatible() ? WIN_PLATFORM : X_PLATFORM) + "_" + this.getVersion().getComplete();
        }

        @Override
        protected Configuration clone() {
            Version version = new Version(this.version.major, this.version.minor, this.version.patch, this.version.complete);
            Configuration configuration = new Configuration(this.displayName, this.name, version, this.windowsCompatible, this.linuxCompatible, this.description, this.feedUrl);
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
