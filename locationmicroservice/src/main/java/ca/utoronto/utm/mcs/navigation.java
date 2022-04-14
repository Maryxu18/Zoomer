package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import org.json.*;
import org.neo4j.driver.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.neo4j.driver.Record;

import static org.neo4j.driver.Values.parameters;

public class navigation implements HttpHandler {

    @Override
    public void handle(HttpExchange r) throws IOException {
        try {
            if (r.getRequestMethod().equals("GET")) {
                getNavigation(r);
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

    private void getNavigation(HttpExchange r) throws IOException, JSONException {
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
        String[] passengerSplitter = paramsSplitter[1].split("=");
        if (passengerSplitter.length != 2 || !passengerSplitter[0].equals("passengerUid")) {
            statusResponse(r, 400, "BAD REQUEST");
            return;
        }
        String driverUid = paramsSplitter[0];
        String passengerUid = passengerSplitter[1];

        JSONObject res = new JSONObject();
        if (driverUid.isEmpty() || passengerUid.isEmpty()) {
            statusResponse(r, 400, "BAD REQUEST");
        } else {

            String getNavigationQuery = """
                    MATCH (p:user {uid: $x})
                    MATCH (d:user {uid: $y, is_driver: true})\s
                    MATCH (s:road {name: d.street_at})
                    MATCH (e:road {name: p.street_at})
                    MATCH path=(s)-[:ROUTE_TO*]-(e)
                    WHERE NOT (p.uid = d.uid)
                    WITH path,reduce(x=0, r IN relationships(path) | x+r.travel_time) AS time
                    RETURN nodes(path), relationships(path), time ORDER BY time LIMIT 1""";

            try (Session session = Utils.driver.session()) {
                Result result = session.run(getNavigationQuery, parameters("x", passengerUid, "y", driverUid));
                if (!result.hasNext()){
                    statusResponse(r, 404, "NOT FOUND");
                } else {
                    JSONObject data = new JSONObject();
                    Record path = result.next();
                    Long time = path.get("time").asLong();
                    List<JSONObject> routes = new LinkedList<>();
                    List<JSONObject> relations = new LinkedList<>();

                    JSONObject initial_data = new JSONObject();
                    initial_data.put("time", 0);
                    initial_data.put("has_traffic", false);
                    relations.add(initial_data);

                    //relations.add(new JSONObject("{time: 0, has_traffic: false}"));
                    for (Value relation: path.get("relationships(path)").values()){
                        JSONObject relation_data = new JSONObject();
                        relation_data.put("time", relation.get("travel_time").asInt());
                        relation_data.put("has_traffic", relation.get("is_traffic").asBoolean());
                        relations.add(relation_data);
                    }

                    int i = 0;

                    for (Value node: path.get("nodes(path)").values()){
                        JSONObject route_data = new JSONObject();
                        route_data.put("street", node.get("name").toString().replace("\"",""));
                        route_data.put("time", relations.get(i).get("time"));
                        route_data.put("has_traffic", relations.get(i).get("has_traffic"));
                        routes.add(route_data);
                        i++;
                    }
                    data.put("total_time", time);
                    data.put("route", routes);
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
