import service.*;
import dto.UserSearchResponse;
import dto.FriendsResponse;
import dto.GroupsResponse;
import dto.WallResponse;
import util.ConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            logger.info("Запуск VK API Client");
            logger.info("Режим работы: {}", ConfigReader.isTestMode() ? "ТЕСТОВЫЙ" : "ПОЛНЫЙ");

            VkApiService vkService = new VkApiService();
            StatisticsService statisticsService = new StatisticsService(vkService);
            ExportService exportService = new ExportService();

            int cityId = ConfigReader.getSearchCityId();
            String cityName = ConfigReader.getSearchCityName();
            int targetUsersCount = ConfigReader.getTargetUsersCount();
            int usersPerRequest = ConfigReader.getSearchUsersPerRequest();

            logger.info("Поиск пользователей из {} (ID: {})", cityName, cityId);
            logger.info("Целевое количество пользователей: {}", targetUsersCount);

            // Сбор пользователей с пагинацией
            List<UserSearchResponse.User> allUsers = collectUsers(vkService, cityId, targetUsersCount, usersPerRequest);

            if (allUsers.isEmpty()) {
                logger.error("Не удалось собрать пользователей для анализа");
                return;
            }

            // Поиск пользователя с максимальным количеством друзей и открытым профилем
            UserSearchResponse.User targetUser = findUserWithMostFriends(vkService, allUsers);

            if (targetUser != null) {
                processUserStatistics(vkService, statisticsService, exportService, targetUser);
            } else {
                logger.warn("Не найден подходящий пользователь с открытым профилем и друзьями");
            }

            logger.info("Программа завершена успешно");

        } catch (Exception e) {
            logger.error("Критическая ошибка при выполнении программы", e);
        }
    }

    private static List<UserSearchResponse.User> collectUsers(VkApiService vkService, int cityId,
                                                              int targetUsersCount, int usersPerRequest) {
        List<UserSearchResponse.User> allUsers = new ArrayList<>();
        int offset = 0;
        int requestCount = 0;
        boolean shouldContinue = true;

        while (allUsers.size() < targetUsersCount && shouldContinue) {
            try {
                logger.info("Запрос пользователей: offset={}, count={}", offset, usersPerRequest);
                UserSearchResponse response = vkService.searchUsers(cityId, offset, usersPerRequest);

                if (response == null || response.response == null || response.response.items.isEmpty()) {
                    logger.warn("Не удалось получить пользователей или список пуст");
                    shouldContinue = false;
                    break;
                }

                // Фильтруем только открытые профили
                List<UserSearchResponse.User> openUsers = new ArrayList<>();
                for (UserSearchResponse.User user : response.response.items) {
                    if (user.is_closed != null && !user.is_closed) {
                        openUsers.add(user);
                    }
                }

                allUsers.addAll(openUsers);
                offset += usersPerRequest;
                requestCount++;

                logger.info("Собрано пользователей: {}/{} (открытых в этом запросе: {})",
                        allUsers.size(), targetUsersCount, openUsers.size());

                // Если в ответе меньше запрошенного количества, значит это последняя страница
                if (response.response.items.size() < usersPerRequest) {
                    logger.info("Достигнут конец списка пользователей");
                    shouldContinue = false;
                    break;
                }

                // Для тестового режима можно выйти раньше
                if (ConfigReader.isTestMode() && allUsers.size() >= ConfigReader.getTestUsersCount()) {
                    logger.info("Достигнуто целевое количество пользователей для тестового режима");
                    break;
                }

                // После каждых 3 запросов делаем дополнительную паузу
                if (requestCount % 3 == 0) {
                    logger.info("Пауза для соблюдения лимитов VK API...");
                    TimeUnit.MILLISECONDS.sleep(2000);
                }

            } catch (VkApiException e) {
                if (e.getErrorCode() == 6 || e.getErrorCode() == 9) {
                    logger.warn("Превышены лимиты VK API (ошибка {}). Делаем паузу 10 секунд...", e.getErrorCode());
                    try {
                        TimeUnit.MILLISECONDS.sleep(10000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        shouldContinue = false;
                    }
                    // Продолжаем с той же позиции
                    continue;
                } else {
                    logger.error("Критическая ошибка VK API: {}", e.getMessage());
                    shouldContinue = false;
                }
            } catch (Exception e) {
                logger.error("Ошибка при сборе пользователей: {}", e.getMessage());
                shouldContinue = false;
            }
        }

        logger.info("Всего собрано открытых профилей: {}", allUsers.size());
        return allUsers;
    }

    private static UserSearchResponse.User findUserWithMostFriends(VkApiService vkService,
                                                                   List<UserSearchResponse.User> users) {
        UserSearchResponse.User bestUser = null;
        int maxFriends = -1;
        int processedUsers = 0;
        int successCount = 0;

        logger.info("Поиск пользователя с максимальным количеством друзей среди {} пользователей...", users.size());

        for (UserSearchResponse.User user : users) {
            try {
                // Добавляем дополнительную задержку между запросами друзей
                if (processedUsers > 0) {
                    TimeUnit.MILLISECONDS.sleep(1000); // Дополнительная задержка 1 секунда
                }

                FriendsResponse friendsResponse = vkService.getFriends(user.id);
                if (friendsResponse != null && friendsResponse.response != null) {
                    int friendsCount = friendsResponse.response.count;
                    successCount++;

                    if (friendsCount > maxFriends) {
                        maxFriends = friendsCount;
                        bestUser = user;
                        logger.info("Найден пользователь с {} друзьями: {} {}",
                                friendsCount, user.first_name, user.last_name);
                    }
                }
                processedUsers++;

                // В тестовом режиме ограничиваем количество обрабатываемых пользователей
                if (ConfigReader.isTestMode() && processedUsers >= 5) {
                    logger.info("Тестовый режим: ограничение в {} пользователей достигнуто", processedUsers);
                    break;
                }

                // Ограничиваем общее количество успешных запросов для избежания flood control
                if (successCount >= 20) {
                    logger.info("Достигнуто ограничение в {} успешных запросов друзей", successCount);
                    break;
                }

            } catch (VkApiException e) {
                if (e.getErrorCode() == 6 || e.getErrorCode() == 9) {
                    logger.warn("Пропускаем пользователя {} из-за ограничений VK API (ошибка {}): {}",
                            user.id, e.getErrorCode(), e.getMessage());
                    // Делаем паузу перед следующим пользователем
                    try {
                        TimeUnit.MILLISECONDS.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                } else {
                    logger.warn("Не удалось получить друзей для пользователя {}: {}", user.id, e.getMessage());
                }
            } catch (Exception e) {
                logger.warn("Не удалось получить друзей для пользователя {}: {}", user.id, e.getMessage());
            }
        }

        if (bestUser != null) {
            logger.info("Выбран пользователь: {} {} (ID: {}) с {} друзьями",
                    bestUser.first_name, bestUser.last_name, bestUser.id, maxFriends);
        } else {
            logger.warn("Не удалось найти подходящего пользователя с друзьями");
        }

        return bestUser;
    }

    private static void processUserStatistics(VkApiService vkService, StatisticsService statisticsService,
                                              ExportService exportService, UserSearchResponse.User user) {
        String userName = user.first_name + " " + user.last_name;
        logger.info("Обработка пользователя: {} (ID: {})", userName, user.id);

        try {
            // Расчет статистики
            UserStatistics statistics = statisticsService.calculateStatistics(user.id, userName);

            // Вывод статистики в консоль
            printStatistics(statistics);

            // Экспорт в JSON
            if (ConfigReader.isExportJsonEnabled()) {
                exportService.exportToJson(statistics, user.id);
            }

            // Экспорт в CSV
            if (ConfigReader.isExportCsvEnabled()) {
                try {
                    FriendsResponse friendsResponse = vkService.getFriends(user.id);
                    exportService.exportFriendsToCsv(friendsResponse, user.id);
                } catch (Exception e) {
                    logger.warn("Не удалось экспортировать друзей в CSV: {}", e.getMessage());
                }

                try {
                    GroupsResponse groupsResponse = vkService.getGroups(user.id);
                    exportService.exportGroupsToCsv(groupsResponse, user.id);
                } catch (Exception e) {
                    logger.warn("Не удалось экспортировать группы в CSV: {}", e.getMessage());
                }
            }

        } catch (VkApiException e) {
            if (e.getErrorCode() == 6 || e.getErrorCode() == 9) {
                logger.error("Не удалось собрать статистику из-за ограничений VK API. Попробуйте позже.");
            } else {
                logger.error("Ошибка VK API при сборе статистики: {}", e.getMessage());
            }
        } catch (Exception e) {
            logger.error("Ошибка при обработке статистики пользователя: {}", e.getMessage());
        }
    }

    private static void printStatistics(UserStatistics statistics) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("СТАТИСТИКА ПОЛЬЗОВАТЕЛЯ: " + statistics.getUser().getName() +
                " (id: " + statistics.getUser().getId() + ")");
        System.out.println("=".repeat(80));

        printFriendsStatistics(statistics.getFriends());
        printGroupsStatistics(statistics.getGroups());
        printPostsStatistics(statistics.getPosts());
    }

    private static void printFriendsStatistics(UserStatistics.FriendsStatistics friends) {
        System.out.println("\n[ДРУЗЬЯ]");
        System.out.println("Всего друзей: " + friends.getTotal());

        if (friends.getTopCities() != null && !friends.getTopCities().isEmpty()) {
            System.out.println("Топ-5 городов:");
            int rank = 1;
            for (var city : friends.getTopCities()) {
                double percentage = friends.getTotal() > 0 ?
                        (double) city.getValue() / friends.getTotal() * 100 : 0;
                System.out.printf("  %d. %s: %d друзей (%.1f%%)%n",
                        rank++, city.getKey(), city.getValue(), percentage);
            }
        } else {
            System.out.println("Информация о городах друзей недоступна");
        }

        System.out.printf("Процент закрытых профилей: %.1f%%%n", friends.getClosedPercentage());
        System.out.printf("Среднее количество друзей у друзей: %.1f%n", friends.getAvgFriendsOfFriends());
    }

    private static void printGroupsStatistics(UserStatistics.GroupsStatistics groups) {
        System.out.println("\n[ГРУППЫ]");
        System.out.println("Всего групп: " + groups.getTotal());

        if (groups.getTotal() > 0) {
            System.out.printf("Средний размер группы: %.0f участников%n", groups.getAvgSize());

            if (groups.getMinGroup() != null && groups.getMaxGroup() != null) {
                System.out.println("Самая маленькая группа: " + groups.getMinGroup().name +
                        " (" + groups.getMinGroup().members_count + " участников)");
                System.out.println("Самая большая группа: " + groups.getMaxGroup().name +
                        " (" + groups.getMaxGroup().members_count + " участников)");
            }

            if (groups.getTopGroups() != null && !groups.getTopGroups().isEmpty()) {
                System.out.println("Топ-10 самых популярных групп:");
                int rank = 1;
                for (var group : groups.getTopGroups()) {
                    System.out.printf("  %d. %s (%d участников)%n",
                            rank++, group.name, group.members_count);
                }
            }
        } else {
            System.out.println("Пользователь не состоит в группах или информация недоступна");
        }
    }

    private static void printPostsStatistics(UserStatistics.PostsStatistics posts) {
        System.out.println("\n[ПОСТЫ]");
        System.out.println("Всего постов: " + posts.getTotal());

        if (posts.getTotal() > 0) {
            System.out.printf("Средняя активность: %.1f реакций на пост%n", posts.getAvgActivity());
            System.out.printf("Средняя длина текста: %.1f символов%n", posts.getAvgTextLength());

            if (posts.getTopPosts() != null && !posts.getTopPosts().isEmpty()) {
                System.out.println("Топ-3 самых популярных поста:");
                int rank = 1;
                for (var post : posts.getTopPosts()) {
                    int activity = (post.comments != null ? post.comments.count : 0) +
                            (post.likes != null ? post.likes.count : 0) +
                            (post.reposts != null ? post.reposts.count : 0);
                    String preview = post.text != null && post.text.length() > 50 ?
                            post.text.substring(0, 50) + "..." : (post.text != null ? post.text : "без текста");
                    System.out.printf("  %d. \"%s\" (%d лайков, %d репостов, %d комментариев)%n",
                            rank++, preview,
                            post.likes != null ? post.likes.count : 0,
                            post.reposts != null ? post.reposts.count : 0,
                            post.comments != null ? post.comments.count : 0);
                }
            }

            if (posts.getPostTypeDistribution() != null && !posts.getPostTypeDistribution().isEmpty()) {
                System.out.println("Распределение постов по типам:");
                posts.getPostTypeDistribution().forEach((type, count) -> {
                    System.out.printf("  - %s: %d%n", type, count);
                });
            }
        } else {
            System.out.println("У пользователя нет постов или стена закрыта");
        }
    }
}