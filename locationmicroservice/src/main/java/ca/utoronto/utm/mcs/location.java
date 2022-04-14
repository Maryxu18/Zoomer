package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;

import org.json.*;
import org.neo4j.driver.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.neo4j.driver.Record;

import static org.neo4j.driver.Values.parameters;

public class location implements HttpHandler {
   @Override
   public void handle(HttpExchange r) throws IOException {
      try {
         if (r.getRequestMethod().equals("GET")) {
            getLocation(r);
         } else if (r.getRequestMethod().equals("PATCH")) {
            patchLocation(r);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   private void getLocation(HttpExchange r) throws IOException, JSONException {
      int statusCode = 400;
      String requestURI = r.getRequestURI().toString();
      String[] uriSplitter = requestURI.split("/");
      // if there are extra url params send 400 and return
      if (uriSplitter.length != 3) {
         JSONObject data = new JSONObject();
         data.put("status", "BAD REQUEST");
         String response = data.toString();
         r.sendResponseHeaders(statusCode, response.length());
         OutputStream os = r.getResponseBody();
         os.write(response.getBytes());
         os.close();
         return;
      }
      String uid = uriSplitter[2];
      JSONObject res = new JSONObject();
      if (uid.isEmpty()) {

         // no uid provided
         res.put("status", "BAD REQUEST");
         String response = res.toString();
         r.sendResponseHeaders(statusCode, response.length());
         OutputStream os = r.getResponseBody();
         os.write(response.getBytes());
         os.close();
      } else {

         String getLocationQuery = "MATCH (n: user {uid :$x}) RETURN n.longitude,n.latitude,n.street_at";

         try (Session session = Utils.driver.session()) {
            Result result = session.run(getLocationQuery, parameters("x", uid));
            if (result.hasNext()) {

               Record user = result.next();
               Double longitude = user.get("n.longitude").asDouble();
               Double latitude = user.get("n.latitude").asDouble();
               String street = user.get("n.street_at").asString();

               statusCode = 200;
               JSONObject data = new JSONObject();
               data.put("longitude", longitude);
               data.put("latitude", latitude);
               data.put("street", street);
               res.put("status", "OK");
               res.put("data", data);
               String response = res.toString();
               r.sendResponseHeaders(statusCode, response.length());
               OutputStream os = r.getResponseBody();
               os.write(response.getBytes());
               os.close();
            } else {
               statusCode = 404;
               JSONObject data = new JSONObject();
               data.put("status", "NOT FOUND");
               String response = data.toString();
               r.sendResponseHeaders(statusCode, response.length());
               OutputStream os = r.getResponseBody();
               os.write(response.getBytes());
               os.close();
            }
         } catch (Exception e) {
            statusCode = 500;
            JSONObject data = new JSONObject();
            data.put("status", "INTERNAL SERVER ERROR");
            String response = data.toString();
            r.sendResponseHeaders(statusCode, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
         }
      }
   }

   private void patchLocation(HttpExchange r) throws IOException, JSONException {
      int statusCode = 400;
      String requestURI = r.getRequestURI().toString();
      String[] uriSplitter = requestURI.split("/");
      // if there are extra url params send 400 and return
      if (uriSplitter.length != 3) {
         JSONObject data = new JSONObject();
         data.put("status", "BAD REQUEST");
         String response = data.toString();
         r.sendResponseHeaders(statusCode, response.length());
         OutputStream os = r.getResponseBody();
         os.write(response.getBytes());
         os.close();
         return;
      }
      String uid = uriSplitter[2];
      String body = Utils.convert(r.getRequestBody());
      JSONObject res = new JSONObject();
      JSONObject req = new JSONObject(body);
      if (uid.isEmpty()) {
         // no uid provided
         res.put("status", "BAD REQUEST");
         String response = res.toString();
         r.sendResponseHeaders(statusCode, response.length());
         OutputStream os = r.getResponseBody();
         os.write(response.getBytes());
         os.close();
      } else {
         try (Session session = Utils.driver.session()) {
            String get_user = "MATCH (n: user {uid :$x}) RETURN n";
            Result result = session.run(get_user, parameters("x", uid));
            if (result.hasNext()) {
               // user found
               Result update = session.run(
                     "MATCH(n: user {uid: $t}) SET n.longitude = $x, n.latitude = $y, n.street_at = $z RETURN n",
                     parameters("t", uid, "x", req.getDouble("longitude"), "y", req.getDouble("latitude"), "z",
                           req.getString("street")));
               if (update.hasNext()) {
                  // update successfully
                  statusCode = 200;
                  res.put("status", "OK");
                  String response = res.toString();
                  r.sendResponseHeaders(statusCode, response.length());
                  OutputStream os = r.getResponseBody();
                  os.write(response.getBytes());
                  os.close();
               } else {
                  statusCode = 500;
                  res.put("status", "INTERNAL SERVER ERROR");
                  String response = res.toString();
                  r.sendResponseHeaders(statusCode, response.length());
                  OutputStream os = r.getResponseBody();
                  os.write(response.getBytes());
                  os.close();
               }
            } else {
               statusCode = 404;
               JSONObject data = new JSONObject();
               data.put("status", "NOT FOUND");
               String response = data.toString();
               r.sendResponseHeaders(statusCode, response.length());
               OutputStream os = r.getResponseBody();
               os.write(response.getBytes());
               os.close();
            }
         } catch (Exception e) {
            statusCode = 500;
            res.put("status", "INTERNAL SERVER ERROR");
            String response = res.toString();
            r.sendResponseHeaders(statusCode, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
         }
      }
   }
}
