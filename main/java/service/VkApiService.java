package service;

import dto.*;
import util.ConfigReader;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

public class VkApiService {
    private static final Logger logger = LoggerFactory.getLogger(VkApiService.class);
    private static final String API_URL = "https://api.vk.com/method/";
    private static final String TOKEN = ConfigReader.getToken();
    private static final String VERSION = ConfigReader.getApiVersion();
    private final HttpClient httpClient;
    private final Gson gson;
    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 500; // Увеличили базовую задержку

    public VkApiService() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    public UserSearchResponse searchUsers(int cityId, int offset, int count) throws Exception {
        String url = API_URL + "users.search" +
                "?city=" + cityId +
                "&offset=" + offset +
                "&count=" + count +
                "&fields=city,can_access_closed,is_closed" +
                "&access_token=" + TOKEN +
                "&v=" + VERSION;

        return executeRequestWithRetry(url, UserSearchResponse.class);
    }

    public FriendsResponse getFriends(int userId) throws Exception {
        String url = API_URL + "friends.get" +
                "?user_id=" + userId +
                "&fields=city,counters" +
                "&access_token=" + TOKEN +
                "&v=" + VERSION;

        return executeRequestWithRetry(url, FriendsResponse.class);
    }

    public GroupsResponse getGroups(int userId) throws Exception {
        String url = API_URL + "groups.get" +
                "?user_id=" + userId +
                "&extended=1" +
                "&fields=members_count" +
                "&access_token=" + TOKEN +
                "&v=" + VERSION;

        return executeRequestWithRetry(url, GroupsResponse.class);
    }

    public WallResponse getWall(int userId, int count) throws Exception {
        String url = API_URL + "wall.get" +
                "?owner_id=" + userId +
                "&count=" + count +
                "&filter=owner" +
                "&access_token=" + TOKEN +
                "&v=" + VERSION;

        return executeRequestWithRetry(url, WallResponse.class);
    }

    private <T> T executeRequestWithRetry(String url, Class<T> responseType) throws Exception {
        int retryCount = 0;
        while (retryCount <= MAX_RETRIES) {
            try {
                return executeRequest(url, responseType);
            } catch (VkApiException e) {
                if ((e.getErrorCode() == 6 || e.getErrorCode() == 9) && retryCount < MAX_RETRIES) {
                    retryCount++;
                    long delayMs = calculateBackoffDelay(retryCount, e.getErrorCode());
                    logger.warn("VK API Error [{}]. Retry {}/{} after {} ms",
                            e.getErrorCode(), retryCount, MAX_RETRIES, delayMs);
                    TimeUnit.MILLISECONDS.sleep(delayMs);
                } else {
                    throw e;
                }
            }
        }
        throw new VkApiException("Max retries exceeded", 0);
    }

    private long calculateBackoffDelay(int retryCount, int errorCode) {
        // Для ошибки 9 (Flood control) используем более длинные задержки
        long baseDelay = (errorCode == 9) ? 2000 : 1000;
        return baseDelay * (long) Math.pow(2, retryCount - 1); // Экспоненциальная backoff
    }

    private <T> T executeRequest(String url, Class<T> responseType) throws Exception {
        long startTime = System.currentTimeMillis();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        long duration = System.currentTimeMillis() - startTime;

        // Логирование запроса
        logger.info("GET {} ({}ms, {} chars)",
                url.substring(0, url.indexOf("access_token")) + "...",
                duration, response.body().length());

        if (response.statusCode() != 200) {
            logger.error("HTTP Error: {}", response.statusCode());
            throw new RuntimeException("HTTP error: " + response.statusCode());
        }

        // Проверка на ошибки VK API
        if (response.body().contains("\"error\"")) {
            VkApiError error = gson.fromJson(response.body(), VkApiError.class);
            logger.error("VK API Error [{}]: {}",
                    error.error.error_code,
                    error.error.error_msg);
            throw new VkApiException(error.error.error_msg, error.error.error_code);
        }

        // Увеличили базовую задержку между запросами
        TimeUnit.MILLISECONDS.sleep(BASE_DELAY_MS);

        return gson.fromJson(response.body(), responseType);
    }

    // Класс для обработки ошибок VK API
    private static class VkApiError {
        public Error error;

        public static class Error {
            public int error_code;
            public String error_msg;
        }
    }
}