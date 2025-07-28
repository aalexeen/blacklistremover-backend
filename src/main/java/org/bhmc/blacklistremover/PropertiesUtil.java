package org.bhmc.blacklistremover;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class PropertiesUtil {

    private static final Properties properties = new Properties();

    static {
        String resourceFileName = "config.properties";
        try (InputStream input = PropertiesUtil.class.getClassLoader().getResourceAsStream(resourceFileName)) {
            if (input == null) {
                throw new IllegalArgumentException("Resource file not found: " + resourceFileName);
            }
            properties.load(input);
        } catch (IOException e) {
            System.err.println("Error loading properties file: " + resourceFileName + ": " + e.getMessage());
            throw new RuntimeException("Failed to load properties: " + e.getMessage(), e);
        }
    }

    private PropertiesUtil() {
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
