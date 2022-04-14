package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;

import org.json.*;
import org.neo4j.driver.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import static org.neo4j.driver.Values.parameters;

public class route implements HttpHandler {
   @Override
   public void handle(HttpExchange r) throws IOException {
      try {
         if (r.getRequestMethod().equals("POST")) {
            routeAdd(r);
         } else if (r.getRequestMethod().equals("DELETE")) {
            routeDelete(r);
         }
      } catch (Exception e) {
         System.out.println("Error Occurred! Msg:   " + e);
      }
   }

   private void routeAdd(HttpExchange r) throws IOException, JSONException {
      String body = Utils.convert(r.getRequestBody());
      JSONObject res = new JSONObject();
      JSONObject req = new JSONObject(body);
      int statusCode = 400;
      if (req.has("roadName1") && req.has("roadName2") && req.has("hasTraffic") && req.has("time")) {
         try (Session session = Utils.driver.session()) {
            String road1 = req.getString("roadName1");
            String road2 = req.getString("roadName2");
            Boolean isTraffic = req.getBoolean("hasTraffic");
            int time = req.getInt("time");
            String preparedStatement = "MATCH (r1:road {name: $x}), (r2:road {name: $y}) "
                  + "CREATE (r1) -[r:ROUTE_TO {travel_time: $z, is_traffic: $u}]->(r2) RETURN type(r)";
            Result result = session.run(preparedStatement,
                  parameters("x", road1, "y", road2, "u", isTraffic, "z", time));
            if (result.hasNext()) {
               // relationship created
               statusCode = 200;
               res.put("status", "OK");
            } else {
               statusCode = 500;
               res.put("status", "INTERNAL SERVER ERROR");
            }
         } catch (Exception e) {
            statusCode = 500;
            res.put("status", "INTERNAL SERVER ERROR");
         }
      } else {
         res.put("status", "BAD REQUEST");
      }
      String response = res.toString();
      r.sendResponseHeaders(statusCode, response.length());
      OutputStream os = r.getResponseBody();
      os.write(response.getBytes());
      os.close();
   }

   private void routeDelete(HttpExchange r) throws IOException, JSONException {
      String body = Utils.convert(r.getRequestBody());
      JSONObject res = new JSONObject();
      JSONObject req = new JSONObject(body);
      int statusCode = 400;
      if (req.has("roadName1") && req.has("roadName2")) {
         try (Session session = Utils.driver.session()) {
            String road1 = req.getString("roadName1");
            String road2 = req.getString("roadName2");
            String preparedStatement = "MATCH (r1:road {name: $x})-[r:ROUTE_TO]->(r2:road {name: $y}) " + "DELETE r";
            // relationship deletion action
            session.run(preparedStatement, parameters("x", road1, "y", road2));
            statusCode = 200;
            res.put("status", "OK");
         } catch (Exception e) {
            statusCode = 500;
            res.put("status", "INTERNAL SERVER ERROR");
         }
      } else {
         res.put("status", "BAD REQUEST");
      }
      String response = res.toString();
      r.sendResponseHeaders(statusCode, response.length());
      OutputStream os = r.getResponseBody();
      os.write(response.getBytes());
      os.close();
   }
}
