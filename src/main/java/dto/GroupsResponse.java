package dto;

import java.util.List;

public class GroupsResponse {
    public Response response;

    public static class Response {
        public Integer count;
        public List<Group> items;
    }

    public static class Group {
        public Integer id;
        public String name;
        public String screen_name;
        public Integer members_count;
        public String type;
    }
}