package ca.utoronto.utm.mcs;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.JSONObject;
import org.junit.jupiter.api.*;

import org.json.JSONException;

import javax.print.attribute.standard.JobName;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;


/*
Please Write Your Tests For CI/CD In This Class. 
You will see these tests pass/fail on github under github actions.
*/
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AppTest {

    public static HttpClient httpClient;
    public static HttpResponse<String> httpResponse;
    public static HttpRequest httpRequest;
    public static String tripId = "";

    @BeforeAll
    public static void setup() {
        httpClient = HttpClient.newHttpClient();
        // populate database
        try {
            sendRequest("http://localhost:8004/location/user", "PUT", "{\"uid\":\"1\", \"is_driver\": true}");
            sendRequest("http://localhost:8004/location/user", "PUT", "{\"uid\":\"2\", \"is_driver\": false}");
            sendRequest("http://localhost:8004/location/1", "PATCH", "{\"longitude\":50, \"latitude\":50, \"street\":\"street1\"}");
            sendRequest("http://localhost:8004/location/2", "PATCH", "{\"longitude\":50, \"latitude\":52, \"street\":\"street2\"}");
            sendRequest("http://localhost:8004/location/road", "PUT", "{\"roadName\":\"street1\", \"hasTraffic\":false}");
            sendRequest("http://localhost:8004/location/road", "PUT", "{\"roadName\":\"street2\", \"hasTraffic\":true}");
            sendRequest("http://localhost:8004/location/hasRoute", "POST", "{\"roadName1\":\"street1\", \"roadName2\":\"street2\", \"hasTraffic\":true, \"time\":8}");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    @Order(1)
    public void test200TripRequest() {
        try {
            //todo fix
            JSONObject res = sendRequest("http://localhost:8004/trip/request", "POST", "{\"uid\":\"2\",\"radius\":300}");
            assertEquals(200, res.getInt("status"));
            JSONObject expectedResponse = new JSONObject("{\"data\":[\"1\"],\"status\":\"OK\"}");
            assertEquals(res.getString("message"), expectedResponse.toString());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    @Order(2)
    public void test404TripRequest() {
        try {
            JSONObject res = sendRequest("http://localhost:8004/trip/request", "POST", "{\"uid\":\"2\",\"radius\":10}");
            assertEquals(404, res.getInt("status"));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    @Order(3)
    public void test200TripConfirm() {
        try {
            JSONObject res = sendRequest("http://localhost:8004/trip/confirm", "POST", "{\"driver\":\"1\",\"passenger\":\"2\",\"startTime\":1628247600}");
            assertEquals(200, res.getInt("status"));
            JSONObject body = new JSONObject(res.get("message").toString());
            tripId = body.getString("data");
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    @Order(4)
    public void test400TripConfirm() {
        try {
            JSONObject res = sendRequest("http://localhost:8004/trip/confirm", "POST", "{\"driver\":\"1\",\"passenger\":\"2\"}");
            assertEquals(400, res.getInt("status"));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    @Order(5)
    public void test200TripId() {
        try {
            JSONObject reqBody = new JSONObject();
            reqBody.put("distance", 8)
                    .put("endTime", 1628248080)
                    .put("timeElapsed", "00:08:00")
                    .put("discount", 0)
                    .put("totalCost", 10)
                    .put("driverPayout", 6.5);
            JSONObject res = sendRequest("http://localhost:8004/trip/" + tripId, "PATCH", reqBody.toString());
            assertEquals(200, res.getInt("status"));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    @Order(6)
    public void test400TripId() {
        try {
            JSONObject reqBody = new JSONObject();
            reqBody.put("distance", 8);
            JSONObject res = sendRequest("http://localhost:8004/trip/" + tripId, "PATCH", reqBody.toString());
            assertEquals(400, res.getInt("status"));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    @Order(7)
    public void test200GETPassenger() {
        // Already have a passenger(uid = 2) with existing trips
        try {
            JSONObject res = sendRequest("http://localhost:8004/trip/passenger/2", "GET", "");
            assertEquals(200, res.getInt("status"));
            JSONObject intermediate = new JSONObject(res.getString("message"));
            assertEquals("{\"trips\":[{\"timeElapsed\":\"00:08:00\",\"distance\":8,\"driver\":\"1\",\"discount\":0,\"startTime\":1628247600,\"_id\":\""
                    + tripId + "\",\"endTime\":1628248080,\"totalCost\":10}]}", intermediate.getString("data"));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    @Order(8)
    public void test404GETPassenger() {
        // Does not have a passenger(uid = 20000) with existing trips
        try {
            JSONObject res = sendRequest("http://localhost:8004/trip/passenger/20000", "GET", "");
            assertEquals(404, res.getInt("status"));
            JSONObject intermediate = new JSONObject(res.getString("message"));
            String dataResponse = intermediate.getString("data");
            assertEquals("{}", dataResponse);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    @Order(9)
    public void test200GETDriver() {
        // Already have a driver(uid = 1) with existing trips
        try {
            JSONObject res = sendRequest("http://localhost:8004/trip/driver/1", "GET", "");
            assertEquals(200, res.getInt("status"));
            JSONObject intermediate = new JSONObject(res.getString("message"));
            assertEquals("{\"trips\":[{\"timeElapsed\":\"00:08:00\",\"distance\":8,\"passenger\":\"2\",\"driverPayout\":6.5,\"startTime\":1628247600,\"_id\":\""
                    + tripId + "\",\"endTime\":1628248080}]}", intermediate.getString("data"));
            System.out.println("in test, tripid: "+ tripId);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    @Order(10)
    public void test404GETDriver() {
        // Does not have a driver(uid = 20000) with existing trips
        try {
            JSONObject res = sendRequest("http://localhost:8004/trip/driver/20000", "GET", "");
            assertEquals(404, res.getInt("status"));
            JSONObject intermediate = new JSONObject(res.getString("message"));
            String dataResponse = intermediate.getString("data");
            assertEquals("{}", dataResponse);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    @Order(11)
    public void test200GETDriverTime() {
        // this.tripId has an existing trip id
        try {
            JSONObject res = sendRequest("http://localhost:8004/trip/driverTime/" + tripId, "GET", "");
            assertEquals(200, res.getInt("status"));
            JSONObject intermediate = new JSONObject(res.getString("message"));
            JSONObject dataResponse = new JSONObject(intermediate.getString("data"));
            assertEquals(8, dataResponse.getInt("arrival_time"));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    @Order(12)
    public void test404GETDriverTime() {
        // trip id 1234 does not exist
        try {
            JSONObject res = sendRequest("http://localhost:8004/trip/driverTime/1234", "GET", "");
            assertEquals(404, res.getInt("status"));
            JSONObject intermediate = new JSONObject(res.getString("message"));
            String dataResponse = intermediate.getString("data");
            assertEquals(dataResponse, "{}");
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @AfterAll
    public static void cleanup() {
        try {
            sendRequest("http://localhost:8004/location/route", "DELETE", "{\"roadName1\":\"street1\",\"roadName2\":\"street2\"}");
            sendRequest("http://localhost:8004/location/user", "DELETE", "{\"uid\":\"1\"}");
            sendRequest("http://localhost:8004/location/user", "DELETE", "{\"uid\":\"2\"}");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static JSONObject sendRequest(String requestURL, String method, String body) throws IOException, JSONException, InterruptedException {
        JSONObject res = new JSONObject();
        if (method.equals("GET")) {
            httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(requestURL))
                    .build();
        } else if (method.equals(("POST"))) {
            httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(requestURL))
                    .method("POST", HttpRequest.BodyPublishers.ofString(body))
                    .build();
        } else if (method.equals(("PUT"))) {
            httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(requestURL))
                    .method("PUT", HttpRequest.BodyPublishers.ofString(body))
                    .build();
        } else if (method.equals(("PATCH"))) {
            httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(requestURL))
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                    .build();
        } else if (method.equals(("DELETE"))) {
            httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(requestURL))
                    .method("DELETE", HttpRequest.BodyPublishers.ofString(body))
                    .build();
        }
        httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        res.put("status", httpResponse.statusCode());
        res.put("message", httpResponse.body());
        return res;
    }

}
