package service;

import dto.FriendsResponse;
import dto.GroupsResponse;
import dto.WallResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.ConfigReader;

import java.util.*;
import java.util.stream.Collectors;

public class StatisticsService {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsService.class);
    private final VkApiService vkApiService;

    public StatisticsService(VkApiService vkApiService) {
        this.vkApiService = vkApiService;
    }

    public UserStatistics calculateStatistics(int userId, String userName) throws Exception {
        logger.info("Calculating statistics for user: {} (id: {})", userName, userId);

        UserStatistics statistics = new UserStatistics();
        statistics.setUser(new UserStatistics.UserInfo(userId, userName));
        statistics.setGeneratedAt(new Date());

        calculateFriendsStatistics(userId, statistics);
        calculateGroupsStatistics(userId, statistics);
        calculatePostsStatistics(userId, statistics);

        return statistics;
    }

    private void calculateFriendsStatistics(int userId, UserStatistics statistics) throws Exception {
        FriendsResponse friendsResponse = vkApiService.getFriends(userId);
        if (friendsResponse == null || friendsResponse.response == null) {
            logger.warn("Не удалось получить друзей для пользователя {}", userId);
            return;
        }

        List<FriendsResponse.Friend> friends = friendsResponse.response.items;
        int totalFriends = friendsResponse.response.count;
        statistics.getFriends().setTotal(totalFriends);

        Map<String, Long> cityCounts = friends.stream()
                .filter(friend -> friend.city != null)
                .collect(Collectors.groupingBy(
                        friend -> friend.city.title,
                        Collectors.counting()
                ));

        List<Map.Entry<String, Long>> topCities = cityCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());
        statistics.getFriends().setTopCities(topCities);

        long closedProfiles = friends.stream()
                .filter(friend -> friend.is_closed != null && friend.is_closed)
                .count();
        double closedPercentage = totalFriends > 0 ? (double) closedProfiles / totalFriends * 100 : 0;
        statistics.getFriends().setClosedPercentage(closedPercentage);

        int sampleSize = Math.min(ConfigReader.getStatisticsFriendsSample(), friends.size());
        double avgFriendsOfFriends = friends.stream()
                .limit(sampleSize)
                .filter(friend -> friend.counters != null && friend.counters.friends != null)
                .mapToInt(friend -> friend.counters.friends)
                .average()
                .orElse(0);
        statistics.getFriends().setAvgFriendsOfFriends(avgFriendsOfFriends);
    }

    private void calculateGroupsStatistics(int userId, UserStatistics statistics) throws Exception {
        GroupsResponse groupsResponse = vkApiService.getGroups(userId);
        if (groupsResponse == null || groupsResponse.response == null) {
            logger.warn("Не удалось получить группы для пользователя {}", userId);
            return;
        }

        List<GroupsResponse.Group> groups = groupsResponse.response.items;
        statistics.getGroups().setTotal(groupsResponse.response.count);

        List<GroupsResponse.Group> topGroups = groups.stream()
                .sorted(Comparator.comparing(group -> group.members_count != null ? group.members_count : 0, Comparator.reverseOrder()))
                .limit(10)
                .collect(Collectors.toList());
        statistics.getGroups().setTopGroups(topGroups);

        double avgSize = groups.stream()
                .filter(group -> group.members_count != null)
                .mapToInt(group -> group.members_count)
                .average()
                .orElse(0);
        statistics.getGroups().setAvgSize(avgSize);

        Optional<GroupsResponse.Group> minGroup = groups.stream()
                .filter(group -> group.members_count != null)
                .min(Comparator.comparing(group -> group.members_count));
        Optional<GroupsResponse.Group> maxGroup = groups.stream()
                .filter(group -> group.members_count != null)
                .max(Comparator.comparing(group -> group.members_count));

        statistics.getGroups().setMinGroup(minGroup.orElse(null));
        statistics.getGroups().setMaxGroup(maxGroup.orElse(null));
    }

    private void calculatePostsStatistics(int userId, UserStatistics statistics) throws Exception {
        WallResponse wallResponse = vkApiService.getWall(userId, ConfigReader.getStatisticsPostsCount());
        if (wallResponse == null || wallResponse.response == null) {
            logger.warn("Не удалось получить посты для пользователя {}", userId);
            return;
        }

        List<WallResponse.Post> posts = wallResponse.response.items;
        statistics.getPosts().setTotal(wallResponse.response.count);

        List<PostWithActivity> postsWithActivity = posts.stream()
                .map(post -> {
                    int activity = (post.comments != null ? post.comments.count : 0) +
                            (post.likes != null ? post.likes.count : 0) +
                            (post.reposts != null ? post.reposts.count : 0);
                    return new PostWithActivity(post, activity);
                })
                .collect(Collectors.toList());

        double avgActivity = postsWithActivity.stream()
                .mapToInt(PostWithActivity::getActivity)
                .average()
                .orElse(0);
        statistics.getPosts().setAvgActivity(avgActivity);

        List<WallResponse.Post> topPosts = postsWithActivity.stream()
                .sorted(Comparator.comparing(PostWithActivity::getActivity).reversed())
                .limit(3)
                .map(PostWithActivity::getPost)
                .collect(Collectors.toList());
        statistics.getPosts().setTopPosts(topPosts);

        double avgTextLength = posts.stream()
                .filter(post -> post.text != null)
                .mapToInt(post -> post.text.length())
                .average()
                .orElse(0);
        statistics.getPosts().setAvgTextLength(avgTextLength);

        Map<String, Long> postTypeDistribution = posts.stream()
                .collect(Collectors.groupingBy(
                        post -> post.post_type != null ? post.post_type : "unknown",
                        Collectors.counting()
                ));
        statistics.getPosts().setPostTypeDistribution(postTypeDistribution);
    }

    private static class PostWithActivity {
        private final WallResponse.Post post;
        private final int activity;

        public PostWithActivity(WallResponse.Post post, int activity) {
            this.post = post;
            this.activity = activity;
        }

        public WallResponse.Post getPost() {
            return post;
        }

        public int getActivity() {
            return activity;
        }
    }
}