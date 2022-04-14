package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;

import org.json.*;
import org.neo4j.driver.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.neo4j.driver.Record;

import static org.neo4j.driver.Values.parameters;

public class nearbyDriver implements HttpHandler {

    @Override
    public void handle(HttpExchange r) throws IOException {
        try {
            if (r.getRequestMethod().equals("GET")) {
                getNearbyDriver(r);
            }
        } catch (Exception e) {
            System.out.println("Error Occurred! Msg:   " + e);
        }
    }

    private void statusResponse(HttpExchange r, int statusCode, String message) throws JSONException, IOException {
        JSONObject data = new JSONObject();
        if (statusCode == 400 || statusCode == 404){
            data.put("data", new JSONObject());
        }
        data.put("status", message);
        String response = data.toString();
        r.sendResponseHeaders(statusCode, response.length());
        OutputStream os = r.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private void getNearbyDriver(HttpExchange r) throws IOException, JSONException {
        String requestURI = r.getRequestURI().toString();
        String[] uriSplitter = requestURI.split("/");
        if (uriSplitter.length != 4) {
            statusResponse(r, 400, "BAD REQUEST");
            return;
        }
        String[] paramsSplitter = uriSplitter[3].split("\\?");
        if (paramsSplitter.length != 2) {
            statusResponse(r, 400, "BAD REQUEST");
            return;
        }
        String[] radiusSplitter = paramsSplitter[1].split("=");
        if (radiusSplitter.length != 2 || !radiusSplitter[0].equals("radius")) {
            statusResponse(r, 400, "BAD REQUEST");
            return;
        }
        String uid = paramsSplitter[0];
        double radius;
        try {
            radius = Double.parseDouble(radiusSplitter[1])*1000.00; //convert to meters
        } catch(Exception e) {
            statusResponse(r, 400, "BAD REQUEST");
            return;
        }
        JSONObject res = new JSONObject();
        if (uid.isEmpty()) {
            statusResponse(r, 400, "BAD REQUEST");
        } else {

            String getNearbyDriverQuery = """
                    MATCH (u:user {uid: $uid})
                    MATCH (d:user {is_driver: true})\s
                    WHERE (distance(point({longitude: u.longitude, latitude: u.latitude}), point({longitude: d.longitude, latitude: d.latitude})) <= $r)
                    AND NOT (d.uid = u.uid)
                    RETURN d.uid, d.longitude, d.latitude, d.street_at""";

            try (Session session = Utils.driver.session()) {
                Result result = session.run(getNearbyDriverQuery, parameters("uid", uid, "r", radius));
                if (!result.hasNext()){
                    statusResponse(r, 404, "NOT FOUND");
                } else {
                    JSONObject data = new JSONObject();
                    while (result.hasNext()){
                        Record driver = result.next();
                        String driver_uid = driver.get("d.uid").asString();
                        Long longitude = driver.get("d.longitude").asLong();
                        Long latitude = driver.get("d.latitude").asLong();
                        String street = driver.get("d.street_at").asString();
                        JSONObject driver_data = new JSONObject();
                        driver_data.put("longitude", longitude);
                        driver_data.put("latitude", latitude);
                        driver_data.put("street", street);
                        data.put(driver_uid, driver_data);
                    }
                    res.put("data", data);
                    res.put("status", "OK");
                    String response = res.toString();
                    r.sendResponseHeaders(200, response.length());
                    OutputStream os = r.getResponseBody();
                    os.write(response.getBytes());
                    os.close();

                }
            } catch (Exception e) {
                statusResponse(r, 500, "INTERNAL SERVER ERROR");
            }
        }
    }
}
