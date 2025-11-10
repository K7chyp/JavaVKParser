package dto;

import java.util.List;

public class WallResponse {
    public Response response;

    public static class Response {
        public Integer count;
        public List<Post> items;
    }

    public static class Post {
        public Integer id;
        public Integer owner_id;
        public String post_type;
        public String text;
        public Comments comments;
        public Likes likes;
        public Reposts reposts;

        public static class Comments {
            public Integer count;
        }

        public static class Likes {
            public Integer count;
        }

        public static class Reposts {
            public Integer count;
        }
    }
}