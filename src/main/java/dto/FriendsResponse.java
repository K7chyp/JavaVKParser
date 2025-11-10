package dto;

import java.util.List;

public class FriendsResponse {
    public Response response;

    public static class Response {
        public Integer count;
        public List<Friend> items;
    }

    public static class Friend {
        public Integer id;
        public String first_name;
        public String last_name;
        public Boolean is_closed;
        public City city;
        public Counters counters;

        public static class City {
            public Integer id;
            public String title;
        }

        public static class Counters {
            public Integer friends;
        }
    }
}