package service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dto.FriendsResponse;
import dto.GroupsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExportService {
    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);
    private final Gson gson;

    public ExportService() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void exportToJson(UserStatistics statistics, int userId) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = String.format("statistics_%d_%s.json", userId, timestamp);

        try (Writer writer = new FileWriter(filename)) {
            gson.toJson(statistics, writer);
        }

        logger.info("Статистика экспортирована в JSON файл: {}", filename);
    }

    public void exportFriendsToCsv(FriendsResponse friendsResponse, int userId) throws IOException {
        if (friendsResponse == null || friendsResponse.response == null) return;

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = String.format("friends_%d_%s.csv", userId, timestamp);

        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("ID,FirstName,LastName,City,FriendsCount\n");

            for (FriendsResponse.Friend friend : friendsResponse.response.items) {
                String city = friend.city != null ? friend.city.title : "";
                String friendsCount = friend.counters != null && friend.counters.friends != null ? String.valueOf(friend.counters.friends) : "";

                writer.write(String.format("%d,%s,%s,%s,%s\n", friend.id, escapeCsv(friend.first_name), escapeCsv(friend.last_name), escapeCsv(city), friendsCount));
            }
        }

        logger.info("Друзья экспортированы в CSV файл: {}", filename);
    }

    public void exportGroupsToCsv(GroupsResponse groupsResponse, int userId) throws IOException {
        if (groupsResponse == null || groupsResponse.response == null) return;

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = String.format("groups_%d_%s.csv", userId, timestamp);

        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("ID,Name,ScreenName,MembersCount,Type\n");

            for (GroupsResponse.Group group : groupsResponse.response.items) {
                writer.write(String.format("%d,%s,%s,%d,%s\n", group.id, escapeCsv(group.name), escapeCsv(group.screen_name), group.members_count != null ? group.members_count : 0, escapeCsv(group.type)));
            }
        }

        logger.info("Группы экспортированы в CSV файл: {}", filename);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}