package org.codelightful.chantico;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.util.Properties;

public class Configuration {
	private static final Logger logger = LoggerFactory.getLogger("config");
	private static String homeDirectory;
	private static Configuration instance;
	private final Properties properties = new Properties();

	/** Allows to obtain the home folder used to store the configuration and other execution files */
	public static String getHome() {
		if (homeDirectory == null) {
			synchronized (Configuration.class) {
				if (homeDirectory == null) {
					homeDirectory = System.getProperty("home");
					if (homeDirectory == null || homeDirectory.isEmpty()) {
						homeDirectory = System.getenv("CHANTICO_HOME");
						if (homeDirectory != null && !homeDirectory.isEmpty()) {
							logger.trace("Using the home directory at: {}", homeDirectory);
						} else {
							String targetType = "home directory";
							String targetDirectory = System.getProperty("user.home");
							if (targetDirectory == null || targetDirectory.isEmpty()) {
								targetType = "temporal directory";
								targetDirectory = System.getProperty("java.io.tmpdir");
							}
							if (targetDirectory == null || targetDirectory.isEmpty()) {
								logger.error("No home directory is set and the system was unable to determine another location. " +
										"Please set the CHANTICO_HOME environment variable and run the application again");
								throw new RuntimeException("No home directory is set");
							} else {
								homeDirectory = Paths.get(targetDirectory, "/.chantico").toString();
								logger.warn("No home directory defined. The user's {} will be used as home: {}", targetType, homeDirectory);
							}
						}
					}
				}
			}
		}
		return homeDirectory;
	}

	/**
	 * Allows to obtain a file located under the home folder or any other subfolder under it
	 * @param location Vararg with the path pieces
	 */
	public static File getFileFromHome(String... location) {
		return Paths.get(Configuration.getHome(), location).toFile();
	}

	/** Obtains the singleton instance of the configuration */
	public static Configuration getInstance() {
		if (instance == null) {
			synchronized (Configuration.class) {
				if (instance == null) {
					instance = new Configuration();
					instance.load();
				}
			}
		}
		return instance;
	}

	private File getConfigurationFile() {
		return Paths.get(getHome(), "config", "chantico.properties").toFile();
	}

	/** Loads the configuration from the filesystem */
	private synchronized void load() {
		if (!properties.isEmpty()) {
			properties.clear();
		}
		File configFile = getConfigurationFile();
		try {
			if (!configFile.exists()) {
					configFile.getParentFile().mkdirs();
				configFile.createNewFile();
			} else {
				try (FileInputStream stream = new FileInputStream(configFile)) {
					properties.load(stream);
				}
			}
		} catch (Exception ex) {
			logger.error("An error has occurred trying to load the configuration file. location={} cause={}",
					configFile.getAbsolutePath(), ex.getMessage());
			throw new RuntimeException("An error has occurred trying to load the configuration", ex);
		}
	}

	/** Stores the configuration values in the filesystem */
	synchronized void store() {
		File configFile = getConfigurationFile();
		try {
			if (!configFile.exists()) {
				configFile.getParentFile().mkdirs();
				configFile.createNewFile();
			}
			try (FileOutputStream stream = new FileOutputStream(configFile)) {
				properties.store(stream, "Chantico Configuration File");
			}
		} catch (Exception ex) {
			logger.error("An error has occurred trying to save the configuration file. location={} cause={}",
					configFile.getAbsolutePath(), ex.getMessage());
			throw new RuntimeException("An error has occurred trying to save the configuration", ex);
		}
	}

	/**
	 * Sets a configuration value for a specific key
	 * @param key Key or property to set the value for it
	 * @param value Value to set
	 */
	void setValue(String key, String value) {
		if (key != null && !key.isEmpty()) {
			if (value == null && this.properties.containsKey(key)) {
				this.properties.remove(key);
			} else {
				this.properties.put(key, value);
			}
		}
	}

	/**
	 * Get a configuration value as a String
	 * @param key Configuration value to obtain
	 * @param defaultValue Default value to assign if the entry does not exist
	 */
	public String getString(String key, String defaultValue) {
		if (key == null || key.isEmpty()) {
			throw new IllegalArgumentException("Unable to extract a configuration entry without a valid key");
		}
		String value = properties.getProperty(key);
		if (value == null || value.isEmpty()) {
			return defaultValue;
		}
		return value;
	}

	/**
	 * Get a configuration value as an integer
	 * @param key Configuration value to obtain
	 * @param defaultValue Default value to assign if the entry does not exist
	 */
	public int getInt(String key, int defaultValue) {
		String value = getString(key, null);
		if (value != null && !value.isEmpty()) {
			try {
				return Integer.parseInt(value);
			} catch (Exception ex) {
				logger.error("An error has occurred trying to consume a configuration value as an integer. key={} value={}", key, value);
			}
		}
		return defaultValue;
	}

	/**
	 * Get a configuration value as a boolean
	 * @param key Configuration value to obtain
	 * @param defaultValue Default value to assign if the entry does not exist
	 */
	public boolean getBoolean(String key, boolean defaultValue) {
		String value = getString(key, null);
		if (value != null && !value.isEmpty()) {
			try {
				return Boolean.parseBoolean(value);
			} catch (Exception ex) {
				logger.error("An error has occurred trying to consume a configuration value as a boolean. key={} value={}", key, value);
			}
		}
		return defaultValue;
	}
}
