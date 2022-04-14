package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;

import org.json.*;
import org.neo4j.driver.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import static org.neo4j.driver.Values.parameters;

public class user implements HttpHandler {

   @Override
   public void handle(HttpExchange r) throws IOException {
      try {
         if (r.getRequestMethod().equals("PUT")) {
            putUser(r);
         } else if (r.getRequestMethod().equals("DELETE")) {
            delUser(r);
         }
      } catch (Exception e) {
         System.out.println("Error Occurred! Msg:   " + e);
      }

   }

   private void putUser(HttpExchange r) throws IOException, JSONException {
      String body = Utils.convert(r.getRequestBody());
      JSONObject res = new JSONObject();
      JSONObject req = new JSONObject(body);
      int statusCode = 400;
      if (req.has("uid") && req.has("is_driver")) {
         String uid = req.getString("uid");
         Boolean isDriver = req.getBoolean("is_driver");
         try (Session session = Utils.driver.session()) {
            String userCheck = "MATCH (n) where n.uid=" + "'" + uid + "' RETURN n";
            Result userCheckResult = session.run(userCheck);
            if (userCheckResult.hasNext()) {
               // User found, uodate the info
               String update = "MATCH (n:user {uid: $x}) SET n.is_driver = $y RETURN n";
               Result updateRes = session.run(update, parameters("x", uid, "y", isDriver));
               if (updateRes.hasNext()) {
                  statusCode = 200;
                  res.put("status", "OK");
               }
            } else {
               // no user found, add the info as a new user
               String newUserQuery = "CREATE (n: user {uid:$x,is_driver:$y, longitude: 0, latitude: 0, street_at: ''}) RETURN n";
               Result queryResult = session.run(newUserQuery, parameters("x", uid, "y", isDriver));
               if (queryResult.hasNext()) {
                  statusCode = 200;
                  res.put("status", "OK");
               }
            }
            String response = res.toString();
            r.sendResponseHeaders(statusCode, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
         } catch (Exception e) {
            // error happened
            res.put("status", "INTERNAL SERVER ERROR");
            statusCode = 500;
            String response = res.toString();
            r.sendResponseHeaders(statusCode, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
         }
      } else {
         res.put("status", "BAD REQUEST");
         String response = res.toString();
         r.sendResponseHeaders(statusCode, response.length());
         OutputStream os = r.getResponseBody();
         os.write(response.getBytes());
         os.close();
      }

   }

   private void delUser(HttpExchange r) throws IOException, JSONException {
      String body = Utils.convert(r.getRequestBody());
      JSONObject res = new JSONObject();
      JSONObject req = new JSONObject(body);
      int statusCode = 400;
      if (req.has("uid")) {
         try (Session session = Utils.driver.session()) {
            String uid = req.getString("uid");
            // force delete
            String delUserQuery = "MATCH (n: user {uid:$x}) DETACH DELETE n";
            session.run(delUserQuery, parameters("x", uid));
            String sanityCheck = "MATCH (n:user {uid:$x}) RETURN n";
            Result result = session.run(sanityCheck, parameters("x", uid));
            if (!result.hasNext()) {
               // Successfully deleted
               res.put("status", "OK");
               String response = res.toString();
               statusCode = 200;
               r.sendResponseHeaders(statusCode, response.length());
               OutputStream os = r.getResponseBody();
               os.write(response.getBytes());
               os.close();
            } else {
               // Successfully deleted
               res.put("status", "BAD REQUEST");
               String response = res.toString();
               r.sendResponseHeaders(statusCode, response.length());
               OutputStream os = r.getResponseBody();
               os.write(response.getBytes());
               os.close();
            }
         } catch (Exception e) {
            res.put("status", "INTERNAL SERVER ERROR");
            String response = res.toString();
            statusCode = 500;
            r.sendResponseHeaders(statusCode, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
         }
      } else {
         res.put("status", "BAD REQUEST");
         String response = res.toString();
         r.sendResponseHeaders(statusCode, response.length());
         OutputStream os = r.getResponseBody();
         os.write(response.getBytes());
         os.close();
      }
   }
}
