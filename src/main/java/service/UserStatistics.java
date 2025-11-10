package service;

import dto.GroupsResponse;
import dto.WallResponse;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class UserStatistics {
    private UserInfo user;
    private FriendsStatistics friends = new FriendsStatistics();
    private GroupsStatistics groups = new GroupsStatistics();
    private PostsStatistics posts = new PostsStatistics();
    private Date generatedAt;

    public static class UserInfo {
        private final int id;
        private final String name;

        public UserInfo(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    public static class FriendsStatistics {
        private int total;
        private List<Map.Entry<String, Long>> topCities;
        private double closedPercentage;
        private double avgFriendsOfFriends;


        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public List<Map.Entry<String, Long>> getTopCities() {
            return topCities;
        }

        public void setTopCities(List<Map.Entry<String, Long>> topCities) {
            this.topCities = topCities;
        }

        public double getClosedPercentage() {
            return closedPercentage;
        }

        public void setClosedPercentage(double closedPercentage) {
            this.closedPercentage = closedPercentage;
        }

        public double getAvgFriendsOfFriends() {
            return avgFriendsOfFriends;
        }

        public void setAvgFriendsOfFriends(double avgFriendsOfFriends) {
            this.avgFriendsOfFriends = avgFriendsOfFriends;
        }
    }

    public static class GroupsStatistics {
        private int total;
        private List<GroupsResponse.Group> topGroups;
        private double avgSize;
        private GroupsResponse.Group minGroup;
        private GroupsResponse.Group maxGroup;


        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public List<GroupsResponse.Group> getTopGroups() {
            return topGroups;
        }

        public void setTopGroups(List<GroupsResponse.Group> topGroups) {
            this.topGroups = topGroups;
        }

        public double getAvgSize() {
            return avgSize;
        }

        public void setAvgSize(double avgSize) {
            this.avgSize = avgSize;
        }

        public GroupsResponse.Group getMinGroup() {
            return minGroup;
        }

        public void setMinGroup(GroupsResponse.Group minGroup) {
            this.minGroup = minGroup;
        }

        public GroupsResponse.Group getMaxGroup() {
            return maxGroup;
        }

        public void setMaxGroup(GroupsResponse.Group maxGroup) {
            this.maxGroup = maxGroup;
        }
    }

    public static class PostsStatistics {
        private int total;
        private double avgActivity;
        private List<WallResponse.Post> topPosts;
        private double avgTextLength;
        private Map<String, Long> postTypeDistribution;


        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public double getAvgActivity() {
            return avgActivity;
        }

        public void setAvgActivity(double avgActivity) {
            this.avgActivity = avgActivity;
        }

        public List<WallResponse.Post> getTopPosts() {
            return topPosts;
        }

        public void setTopPosts(List<WallResponse.Post> topPosts) {
            this.topPosts = topPosts;
        }

        public double getAvgTextLength() {
            return avgTextLength;
        }

        public void setAvgTextLength(double avgTextLength) {
            this.avgTextLength = avgTextLength;
        }

        public Map<String, Long> getPostTypeDistribution() {
            return postTypeDistribution;
        }

        public void setPostTypeDistribution(Map<String, Long> postTypeDistribution) {
            this.postTypeDistribution = postTypeDistribution;
        }
    }


    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }

    public FriendsStatistics getFriends() {
        return friends;
    }

    public void setFriends(FriendsStatistics friends) {
        this.friends = friends;
    }

    public GroupsStatistics getGroups() {
        return groups;
    }

    public void setGroups(GroupsStatistics groups) {
        this.groups = groups;
    }

    public PostsStatistics getPosts() {
        return posts;
    }

    public void setPosts(PostsStatistics posts) {
        this.posts = posts;
    }

    public Date getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Date generatedAt) {
        this.generatedAt = generatedAt;
    }
}