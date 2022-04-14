
package ca.utoronto.utm.mcs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.JSONObject;
import org.junit.jupiter.api.*;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/*
Please Write Your Tests For CI/CD In This Class. 
You will see these tests pass/fail on github under github actions.
*/
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AppTest {

    @Test
    @Order(1)
    public void testResponse200Register() {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("name", "testUser");
            requestBody.put("email", "test@gmail.com");
            requestBody.put("password", "password123");
            //Create connection
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8004/user/register"))
                    .method("POST", HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();
            HttpResponse<String> httpResponse = HttpClient.newBuilder().build().send(httpRequest, HttpResponse.BodyHandlers.ofString());

            //Get Response
            int responseCode = httpResponse.statusCode();
            assertEquals(200, responseCode);
            System.out.println("in register 200");

        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    @Order(2)
    public void testResponse403Register() {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("name", "testUser");
            requestBody.put("email", "test@gmail.com");
            requestBody.put("password", "password123");
            //Create connection
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8004/user/register"))
                    .method("POST", HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();
            HttpResponse<String> httpResponse = HttpClient.newBuilder().build().send(httpRequest, HttpResponse.BodyHandlers.ofString());

            //Get Response
            int responseCode = httpResponse.statusCode();
            assertEquals(403, responseCode);
            System.out.println("in register 403");

        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    @Order(3)
    public void testResponse200Login() {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("email", "test@gmail.com");
            requestBody.put("password", "password123");
            //Create connection
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8004/user/login"))
                    .method("POST", HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();
            HttpResponse<String> httpResponse = HttpClient.newBuilder().build().send(httpRequest, HttpResponse.BodyHandlers.ofString());

            //Get Response
            int responseCode = httpResponse.statusCode();
            assertEquals(200, responseCode);
            System.out.println("in rlogin 200");

        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    @Order(4)
    public void testResponse401Login() {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("email", "test@gmail.com");
            requestBody.put("password", "wrongPassword");
            //Create connection
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8004/user/login"))
                    .method("POST", HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();
            HttpResponse<String> httpResponse = HttpClient.newBuilder().build().send(httpRequest, HttpResponse.BodyHandlers.ofString());

            //Get Response
            int responseCode = httpResponse.statusCode();
            assertEquals(401, responseCode);
            System.out.println("in login 401");
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }
}
