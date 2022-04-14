package ca.utoronto.utm.mcs;

import static org.junit.jupiter.api.Assertions.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.URI;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/*
Please Write Your Tests For CI/CD In This Class. 
You will see these tests pass/fail on github under github actions.
*/
public class AppTest {

   public static HttpClient httpClient;
   public static HttpResponse<String> httpResponse;
   public static HttpRequest httpRequest;

   @BeforeAll
   public static void setup(){
      httpClient = HttpClient.newHttpClient();

      //populate database
      try{
         sendRequest("http://localhost:8004/location/user", "PUT", "{\"uid\":\"1\", \"is_driver\": true}");
         sendRequest("http://localhost:8004/location/user", "PUT", "{\"uid\":\"2\", \"is_driver\": false}");
         sendRequest("http://localhost:8004/location/1", "PATCH", "{\"longitude\":50, \"latitude\":50, \"street\":\"street1\"}");
         sendRequest("http://localhost:8004/location/2", "PATCH", "{\"longitude\":50, \"latitude\":52, \"street\":\"street2\"}");
         sendRequest("http://localhost:8004/location/road", "PUT", "{\"roadName\":\"street1\", \"hasTraffic\":false}");
         sendRequest("http://localhost:8004/location/road", "PUT", "{\"roadName\":\"street2\", \"hasTraffic\":true}");
         sendRequest("http://localhost:8004/location/hasRoute", "POST", "{\"roadName1\":\"street1\", \"roadName2\":\"street2\", \"hasTraffic\":true, \"time\":8}");
      } catch (Exception e){
         e.printStackTrace();
      }

   }

   @Test
   public void test200GetNearbyDriver() {
      try{
         JSONObject res = sendRequest("http://localhost:8004/location/nearbyDriver/2?radius=225", "GET", "");
         assertEquals(200, res.getInt("status"));
         JSONObject expectedResponse = new JSONObject("{\"data\":{\"1\":{\"street\":\"street1\",\"latitude\":50,\"longitude\":50}},\"status\":\"OK\"}");
         assertEquals(res.getString("message"), expectedResponse.toString());
      } catch (Exception e) {
         e.printStackTrace();
         fail();
      }
   }

   @Test
   public void test404GetNearbyDriver() {
      try{
         JSONObject res = sendRequest("http://localhost:8004/location/nearbyDriver/1?radius=225", "GET", "");
         assertEquals(404, res.getInt("status"));
      } catch (Exception e) {
         e.printStackTrace();
         fail();
      }
   }

   @Test
   public void test200GetNavigation() {
      try{
         JSONObject res = sendRequest("http://localhost:8004/location/navigation/1?passengerUid=2", "GET", "");
         assertEquals(200, res.getInt("status"));
         JSONObject expectedResponse = new JSONObject("{\"data\":{\"route\":[{\"has_traffic\":false,\"street\":\"" +
         "street1\",\"time\":0},{\"has_traffic\":true,\"street\":\"street2\",\"time\":8}],\"total_time\":8},\"" +
         "status\":\"OK\"}");
         assertEquals(res.getString("message"), expectedResponse.toString());
      } catch (Exception e) {
         e.printStackTrace();
         fail();
      }
   }

   @Test
   public void test404GetNavigation() {
      try {
         JSONObject res = sendRequest("http://localhost:8004/location/navigation/1?passengerUid=3", "GET", "");
         assertEquals(404, res.getInt("status"));
      } catch (Exception e){
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
      } catch (Exception e){
         e.printStackTrace();
      }
   }

   public static JSONObject sendRequest(String requestURL, String method, String body) throws IOException, JSONException, InterruptedException {
      JSONObject res = new JSONObject();
      if (method.equals("GET")){
         httpRequest = HttpRequest.newBuilder()
                 .uri(URI.create(requestURL))
                 .build();
      } else if (method.equals(("POST"))){
         httpRequest = HttpRequest.newBuilder()
                 .uri(URI.create(requestURL))
                 .method("POST", HttpRequest.BodyPublishers.ofString(body))
                 .build();
      } else if (method.equals(("PUT"))){
         httpRequest = HttpRequest.newBuilder()
                 .uri(URI.create(requestURL))
                 .method("PUT", HttpRequest.BodyPublishers.ofString(body))
                 .build();
      } else if (method.equals(("PATCH"))){
         httpRequest = HttpRequest.newBuilder()
                 .uri(URI.create(requestURL))
                 .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                 .build();
      }
      httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
      res.put("status", httpResponse.statusCode());
      res.put("message", httpResponse.body());
      return res;
   }
}
