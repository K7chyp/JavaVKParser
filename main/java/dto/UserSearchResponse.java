package dto;

import java.util.List;

public class UserSearchResponse {
    public Response response;

    public static class Response {
        public Integer count;
        public List<User> items;
    }

    public static class User {
        public Integer id;
        public String first_name;
        public String last_name;
        public Boolean is_closed;
        public Boolean can_access_closed;
        public City city;

        public static class City {
            public Integer id;
            public String title;
        }
    }
}