package util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = ConfigReader.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("Не найден config.properties");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка загрузки config.properties", e);
        }
    }

    public static String getToken() {
        return properties.getProperty("vk.api.token");
    }

    public static String getApiVersion() {
        return properties.getProperty("vk.api.version", "5.199");
    }

    public static int getSearchCityId() {
        return Integer.parseInt(properties.getProperty("search.city.id", "1"));
    }

    public static String getSearchCityName() {
        return properties.getProperty("search.city.name", "Москва");
    }

    public static boolean isTestMode() {
        return Boolean.parseBoolean(properties.getProperty("test.mode", "true"));
    }

    public static int getTestUsersCount() {
        return Integer.parseInt(properties.getProperty("test.users.count", "10"));
    }

    public static int getFullUsersCount() {
        return Integer.parseInt(properties.getProperty("full.users.count", "100"));
    }

    public static int getSearchUsersPerRequest() {
        return Integer.parseInt(properties.getProperty("search.users.per_request", "100"));
    }

    public static int getTargetUsersCount() {
        return isTestMode() ? getTestUsersCount() : getFullUsersCount();
    }

    public static int getStatisticsFriendsSample() {
        return Integer.parseInt(properties.getProperty("statistics.friends.sample", "5"));
    }

    public static int getStatisticsPostsCount() {
        return Integer.parseInt(properties.getProperty("statistics.posts.count", "10"));
    }

    public static String getLoggingLevel() {
        return properties.getProperty("logging.level", "INFO");
    }

    public static String getLoggingFile() {
        return properties.getProperty("logging.file", "logs/vk-api-client.log");
    }

    public static boolean isExportJsonEnabled() {
        return Boolean.parseBoolean(properties.getProperty("export.json.enabled", "true"));
    }

    public static boolean isExportCsvEnabled() {
        return Boolean.parseBoolean(properties.getProperty("export.csv.enabled", "false"));
    }

    public static int getRequestMaxRetries() {
        return Integer.parseInt(properties.getProperty("request.max.retries", "3"));
    }

    public static long getRequestBaseDelay() {
        return Long.parseLong(properties.getProperty("request.delay.base", "500"));
    }

    public static int getRequestRetryMultiplier() {
        return Integer.parseInt(properties.getProperty("request.delay.retry.multiplier", "2"));
    }
}