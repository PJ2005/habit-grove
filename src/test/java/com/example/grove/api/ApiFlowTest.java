package com.example.grove.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiFlowTest {

    @Value("${local.server.port}")
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();
    private String base;

    @BeforeEach
    void setUp() {
        base = "http://localhost:" + port;
    }

    // ---------- helpers ----------

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "application/json")
                .method(method, publisher)
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /** Reads the first "id" value from a JSON response body. */
    private long idOf(HttpResponse<String> response) {
        Matcher m = Pattern.compile("\"id\":(\\d+)").matcher(response.body());
        assertTrue(m.find(), "No id in response: " + response.body());
        return Long.parseLong(m.group(1));
    }

    /** Unique email per call, so tests never collide in the shared in-memory database. */
    private long newUser() throws Exception {
        String tag = "u" + System.nanoTime();
        HttpResponse<String> r = send("POST", "/users",
                "{\"name\":\"" + tag + "\",\"email\":\"" + tag + "@example.com\",\"targetProfile\":\"STUDENT\"}");
        assertEquals(201, r.statusCode(), r.body());
        return idOf(r);
    }

    private long newHabit(long userId, String name, String type, int weight) throws Exception {
        HttpResponse<String> r = send("POST", "/users/" + userId + "/habits",
                "{\"name\":\"" + name + "\",\"type\":\"" + type + "\",\"weight\":" + weight + ",\"dailyTarget\":5}");
        assertEquals(201, r.statusCode(), r.body());
        return idOf(r);
    }

    private long newTree(long userId) throws Exception {
        HttpResponse<String> r = send("POST", "/users/" + userId + "/tree",
                "{\"name\":\"Oak\",\"species\":\"OAK\"}");
        assertEquals(201, r.statusCode(), r.body());
        return idOf(r);
    }

    // ---------- tests ----------

    @Test
    void logFlowUpdatesLivingTree() throws Exception {
        long user = newUser();
        long habit = newHabit(user, "Reading", "POSITIVE", 2);
        newTree(user);

        HttpResponse<String> log = send("POST", "/users/" + user + "/logs",
                "{\"habitId\":" + habit + ",\"quantity\":1}");
        assertEquals(201, log.statusCode(), log.body());

        HttpResponse<String> tree = send("GET", "/users/" + user + "/tree", null);
        assertEquals(200, tree.statusCode());
        assertTrue(tree.body().contains("\"growthPoints\":2"), tree.body());
        assertTrue(tree.body().contains("\"todayScore\":20"), tree.body());
        assertTrue(tree.body().contains("\"status\":\"ALIVE\""), tree.body());

        HttpResponse<String> history = send("GET", "/users/" + user + "/tree/history", null);
        assertEquals(200, history.statusCode());
        assertTrue(history.body().contains("\"positivePoints\":2"), history.body());
    }

    @Test
    void userWithoutTreeGets404() throws Exception {
        long user = newUser();
        assertEquals(404, send("GET", "/users/" + user + "/tree", null).statusCode());
    }

    @Test
    void unknownUserGets404() throws Exception {
        assertEquals(404, send("POST", "/users/999999/habits",
                "{\"name\":\"X\",\"type\":\"POSITIVE\",\"weight\":1,\"dailyTarget\":1}").statusCode());
    }

    @Test
    void loggingUnknownHabitGets404() throws Exception {
        long user = newUser();
        newTree(user);
        HttpResponse<String> r = send("POST", "/users/" + user + "/logs",
                "{\"habitId\":999999,\"quantity\":1}");
        assertEquals(404, r.statusCode(), r.body());
    }

    @Test
    void secondLivingTreeIsRejected() throws Exception {
        long user = newUser();
        newTree(user);
        HttpResponse<String> second = send("POST", "/users/" + user + "/tree",
                "{\"name\":\"Pine\",\"species\":\"PINE\"}");
        assertEquals(409, second.statusCode(), second.body());
    }

    @Test
    void memorialListShowsTreeAsAlive() throws Exception {
        long user = newUser();
        newTree(user);
        HttpResponse<String> r = send("GET", "/users/" + user + "/tree/all", null);
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("\"status\":\"ALIVE\""), r.body());
    }

    @Test
    void friendViewHidesHabitNamesUntilOwnerOptsIn() throws Exception {
        long owner = newUser();
        long viewer = newUser();
        long habit = newHabit(owner, "Reading", "POSITIVE", 2);
        newTree(owner);
        send("POST", "/users/" + owner + "/logs", "{\"habitId\":" + habit + ",\"quantity\":1}");

        // Friend request, then acceptance
        assertEquals(201, send("POST", "/users/" + viewer + "/friends/" + owner, null).statusCode());
        assertEquals(200, send("POST", "/users/" + owner + "/friend-requests/" + viewer + "/accept", null).statusCode());

        // Owner shares without habit names
        assertEquals(200, send("PUT", "/users/" + owner + "/tree/shares/" + viewer,
                "{\"showHabitNames\":false}").statusCode());
        HttpResponse<String> hidden = send("GET", "/users/" + viewer + "/friends/" + owner + "/tree", null);
        assertEquals(200, hidden.statusCode(), hidden.body());
        assertTrue(hidden.body().contains("\"recentHabits\":[]"), hidden.body());

        // Owner opts in to habit names
        assertEquals(200, send("PUT", "/users/" + owner + "/tree/shares/" + viewer,
                "{\"showHabitNames\":true}").statusCode());
        HttpResponse<String> shown = send("GET", "/users/" + viewer + "/friends/" + owner + "/tree", null);
        assertTrue(shown.body().contains("\"Reading\""), shown.body());
    }

    @Test
    void strangersCannotViewTree() throws Exception {
        long owner = newUser();
        long stranger = newUser();
        newTree(owner);
        assertEquals(403, send("GET", "/users/" + stranger + "/friends/" + owner + "/tree", null).statusCode());
    }
    
    @Test
    void cannotLogAnotherUsersHabit() throws Exception {
        long owner = newUser();
        long stranger = newUser();
        long habit = newHabit(owner, "Reading", "POSITIVE", 2);
        newTree(stranger);
        HttpResponse<String> r = send("POST", "/users/" + stranger + "/logs",
                "{\"habitId\":" + habit + ",\"quantity\":1}");
        assertEquals(403, r.statusCode(), r.body());
    }
}
