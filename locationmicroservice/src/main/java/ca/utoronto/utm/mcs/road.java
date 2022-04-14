package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;

import org.json.*;
import org.neo4j.driver.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import static org.neo4j.driver.Values.parameters;

public class road implements HttpHandler {
   @Override
   public void handle(HttpExchange r) throws IOException {
      try {
         if (r.getRequestMethod().equals("PUT")) {
            putRoad(r);
         }
      } catch (Exception e) {
         System.out.println("Error Occurred! Msg:   " + e);
      }
   }

   private void putRoad(HttpExchange r) throws IOException, JSONException {
      String body = Utils.convert(r.getRequestBody());
      JSONObject res = new JSONObject();
      JSONObject req = new JSONObject(body);
      int statusCode = 400;
      if (req.has("roadName") && req.has("hasTraffic")) {
         String name = req.getString("roadName");
         Boolean traffic = req.getBoolean("hasTraffic");
         try (Session session = Utils.driver.session()) {
            String roadCheckQuery = "MATCH (n :road) where n.name=" + "'" + name + "' RETURN n";
            Result roadCheckResult = session.run(roadCheckQuery);
            if (roadCheckResult.hasNext()) {
               // Road found, update the info
               String update = "MATCH (n:road {name: '$x'}) SET n.is_traffic = $y RETURN n";
               Result updateRes = session.run(update, parameters("x", name, "y", traffic));
               if (updateRes.hasNext()) {
                  statusCode = 200;
                  res.put("status", "OK");
               }
            } else {
               // no user found, add the info as a new user
               String newRoadQuery = "CREATE (n: road {name:$x,is_traffic:$y}) RETURN n";
               Result newRoadRes = session.run(newRoadQuery, parameters("x", name, "y", traffic));
               if (newRoadRes.hasNext()) {
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
}
