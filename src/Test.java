import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

class Post {
    private int id;
    private String text;
    private List<Comment> commentaries = new ArrayList<>();

    private Post() {
    }

    public Post(int id, String text) {
        this.id = id;
        this.text = text;
    }

    public void addComment(Comment comment) {
        commentaries.add(comment);
    }

    public int getId() {
        return id;
    }
}

class Comment {
    private String user;
    private String text;

    private Comment() {
    }

    public Comment(String user, String text) {
        this.user = user;
        this.text = text;
    }
}

public class Test {
    private static final int PORT = 8080;
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private static Gson gson = new Gson();
    private static List<Post> posts = new ArrayList<>();

    static {
        Post post1 = new Post(1, "Это первый пост, который я здесь написал.");
        post1.addComment(new Comment("Пётр Первый", "Я успел откомментировать первым!"));
        posts.add(post1);

        Post post2 = new Post(22, "Это будет второй пост. Тоже короткий.");
        posts.add(post2);

        Post post3 = new Post(333, "Это пока последний пост.");
        posts.add(post3);
    }

    public static void main(String[] args) throws IOException {
        HttpServer httpServer = HttpServer.create();
        httpServer.bind(new InetSocketAddress(PORT), 0);
        httpServer.createContext("/posts", new PostsHandler());
        httpServer.start();

        System.out.println("HTTP-сервер запущен на " + PORT + " порту!");
        // httpServer.stop(1);
    }

    static class PostsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String response = null;
            Headers headers = httpExchange.getResponseHeaders();
            headers.set("Content-Type", "text/plain");
            String method = httpExchange.getRequestMethod();
            InputStream inputStream = httpExchange.getRequestBody();
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            String body = new String(inputStream.readAllBytes(), DEFAULT_CHARSET);
            System.out.println(body);

            String path = httpExchange.getRequestURI().getPath();
            String[] paths = path.split("/");
            switch (method) {
                case "GET":
                    if (paths.length > 2) {
                        Integer id = Integer.parseInt(paths[2]);
                        boolean foundId = false;
                        for (Post post : posts) {
                            if (post.getId() == id) {
                                foundId = true;
                                response = gson.toJson(post);
                                httpExchange.sendResponseHeaders(200, 0);

                                httpExchange.close();

                                break;

                            }
                        }
                        if (foundId == false) {
                            httpExchange.sendResponseHeaders(404, 0);
                            break;
                        }

                    } else {
                        response = gson.toJson(posts);
                        httpExchange.sendResponseHeaders(200, 0);
                        break;

                    }
                    break;
                case "POST":
                    Integer id = Integer.parseInt(paths[2]);
                    boolean foundId = false;
                    if (id != null) {
                        for (Post post : posts) {
                            if (post.getId() == id) {
                                foundId = true;
                                response = "";
                                String[] comm=body.split(",");
                                        Comment commentDeserialized = new Comment(comm[0],comm[1]);
                                posts.remove(post);
                                post.addComment(commentDeserialized);
                                posts.add(post);
                                httpExchange.sendResponseHeaders(201, 0);
                                break;
                            }
                        }
                        if (foundId == false) {
                            response = "";
                            httpExchange.sendResponseHeaders(404, 0);
                            break;
                        }
                    }
                    break;
                default:
                    System.out.println("default");
                    response = "Вы использовали какой-то другой метод!";
                    httpExchange.sendResponseHeaders(200, 0);
                    break;
            }
            try (OutputStream os = httpExchange.getResponseBody()) {
                os.write(response.getBytes());
                httpExchange.close();
            }
        }
    }
}

