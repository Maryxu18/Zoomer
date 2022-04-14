package ca.utoronto.utm.mcs;


import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.UpdateResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.bson.types.ObjectId;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedList;
import java.util.List;

public class Trip implements HttpHandler {
    public MongoCollection<Document> collection;
    public String response;
    public static HttpClient httpClient;

    public Trip() {
        String uri = "mongodb://root:123456@mongodb:27017";
//        String uri = "mongodb://localhost:27017";
        MongoClient client = new MongoClient(new MongoClientURI(uri));
        MongoDatabase db = client.getDatabase("trip");
//        db.createCollection("trips");
        this.collection = db.getCollection("trips");
        httpClient = HttpClient.newHttpClient();
    }

    @Override
    public void handle(HttpExchange r) {
        try {
            switch (r.getRequestMethod()) {
                case "POST" -> {
                    String requestPath = r.getRequestURI().getPath();
                    if (requestPath.equals("/trip/request")) {
                        handlePOSTRequest(r);
                    } else if (requestPath.equals("/trip/confirm")) {
                        handlePOSTConfirm(r);
                    }
                }
                case "PATCH" -> handlePATCHTrip(r);
                case "GET" -> {
                    String requestPath = r.getRequestURI().getPath();
                    if (requestPath.startsWith("/trip/passenger/")) {
                        handleGETPassenger(r);
                    } else if (requestPath.startsWith("/trip/driver/")) {
                        handleGETDriver(r);
                    } else if (requestPath.startsWith("/trip/driverTime/")) {
                        handleGETDriverTime(r);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handlePOSTRequest(HttpExchange r) throws IOException, JSONException, InterruptedException {
        String body = Utils.convert(r.getRequestBody());
        JSONObject res = new JSONObject();
        JSONObject req = new JSONObject(body);
        if (req.has("uid") && req.has("radius") && Utils.isNumeric(req.get("radius").toString())) {
            String uid = req.getString("uid");
            double radius = req.getDouble("radius");

            //send request to LocationMicroservice
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://locationmicroservice:8000/location/nearbyDriver/".concat(uid).concat("?radius=").concat(Double.toString(radius))))
                    .build();
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            // request succeeded
            if (httpResponse.statusCode() == 200) {
                JSONObject result = new JSONObject(httpResponse.body());
                JSONObject data = result.getJSONObject("data");
                JSONArray names = data.names();
                LinkedList<String> uids = new LinkedList<>();
                for (int i = 0; i < names.length(); ++i) {
                    uids.add(names.getString(i));
                }
                res.put("status", "OK");
                res.put("data", uids);
            } else if (httpResponse.statusCode() == 400) {
                res.put("data", new LinkedList<>());
                res.put("status", "BAD REQUEST");
            } else if (httpResponse.statusCode() == 404) {
                res.put("data", new LinkedList<>());
                res.put("status", "NOT FOUND");
            } else {
                res.put("status", "INTERNAL SERVER ERROR");
            }
            String response = res.toString();
            r.sendResponseHeaders(httpResponse.statusCode(), response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } else {
            res.put("data", new JSONObject());
            res.put("status", "BAD REQUEST");
            String response = res.toString();
            r.sendResponseHeaders(400, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    private void handlePOSTConfirm(HttpExchange r) throws IOException, JSONException {
        String body = Utils.convert(r.getRequestBody());
        JSONObject res = new JSONObject();
        JSONObject req = new JSONObject(body);
        if (req.has("driver") && req.has("passenger") && req.has("startTime") &&
                Utils.isNumeric(req.get("startTime").toString())) {
            String driver = req.getString("driver");
            String passenger = req.getString("passenger");
            if (driver.equals(passenger)){
                res.put("data", "");
                res.put("status", "BAD REQUEST");
                String response = res.toString();
                r.sendResponseHeaders(400, response.length());
                OutputStream os = r.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
            long startTime = req.getLong("startTime");

            ObjectId id = new ObjectId();
            Document tripInfo = new Document("_id", id);
            tripInfo.append("startTime", startTime)
                    .append("driver", driver)
                    .append("passenger", passenger);
            this.collection.insertOne(tripInfo);

            res.put("status", "OK");
            res.put("data", id);
            String response = res.toString();
            r.sendResponseHeaders(200, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } else {
            res.put("data", "");
            res.put("status", "BAD REQUEST");
            String response = res.toString();
            r.sendResponseHeaders(400, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    private void handlePATCHTrip(HttpExchange r) throws IOException, JSONException {
        String body = Utils.convert(r.getRequestBody());
        JSONObject res = new JSONObject();
        JSONObject req = new JSONObject(body);
        String requestURI = r.getRequestURI().toString();
        String[] uriSplitter = requestURI.split("/");
        // if there are extra url params send 400 and return
        if (uriSplitter.length != 3) {
            res.put("status", "BAD REQUEST");
            String response = res.toString();
            r.sendResponseHeaders(400, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
            return;
        }
        String id = uriSplitter[2];
        if (id.isEmpty()) {
            res.put("status", "BAD REQUEST");
            String response = res.toString();
            r.sendResponseHeaders(400, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
            return;
        }
        if (id.getBytes().length != 24) {
            res.put("status", "NOT FOUND");
            String response = res.toString();
            r.sendResponseHeaders(404, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
            return;

        }
        if (req.has("distance") && req.has("endTime") && req.has("timeElapsed") && req.has("discount") &&
                req.has("totalCost") && req.has("driverPayout") &&
                Utils.isNumeric(req.get("distance").toString()) && Utils.isNumeric(req.get("endTime").toString()) &&
                Utils.isNumeric(req.get("discount").toString()) && Utils.isNumeric(req.get("totalCost").toString()) &&
                Utils.isNumeric(req.get("driverPayout").toString())) {

            Bson filter = eq("_id", new ObjectId(id));
            Bson update1 = set("distance", req.getDouble("distance"));
            Bson update2 = set("endTime", req.getLong("endTime"));
            Bson update3 = set("timeElapsed", req.getString("timeElapsed"));
            Bson update4 = set("discount", req.getInt("discount"));
            Bson update5 = set("totalCost", req.getDouble("totalCost"));
            Bson update6 = set("driverPayout", req.getDouble("driverPayout"));
            Bson updates = combine(update1, update2, update3, update4, update5, update6);

            Document findTripInfo = this.collection.findOneAndUpdate(filter, updates);
            if (findTripInfo == null) {
                res.put("status", "NOT FOUND");
                String response = res.toString();
                r.sendResponseHeaders(404, response.length());
                OutputStream os = r.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }
            res.put("status", "OK");
            String response = res.toString();
            r.sendResponseHeaders(200, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } else {
            res.put("status", "BAD REQUEST");
            String response = res.toString();
            r.sendResponseHeaders(400, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    private void handleGETPassenger(HttpExchange r) throws IOException, JSONException {
        String[] url = r.getRequestURI().getPath().split("/");
        String passenger = url[url.length - 1];
        System.out.print("passenger:" + passenger);     //test
        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put("passenger", passenger);
        FindIterable<Document> cursor = collection.find(searchQuery);
        List<JSONObject> paths = new LinkedList<JSONObject>();
        for (Document document : cursor) {
            JSONObject path = new JSONObject();
            path.put("_id", document.get("_id"));
            path.put("distance", document.get("distance"));
            path.put("totalCost", document.get("totalCost"));
            path.put("discount", document.get("discount"));
            path.put("startTime", document.get("startTime"));
            path.put("endTime", document.get("endTime"));
            path.put("timeElapsed", document.get("timeElapsed"));
            path.put("driver", document.get("driver"));
            paths.add(path);
        }

        JSONObject res = new JSONObject();
        if (paths.isEmpty()) {
            res.put("status", "NOT FOUND");
            res.put("data", new JSONObject());
            response = res.toString();
            r.sendResponseHeaders(404, response.length());
        } else {
            JSONObject trips = new JSONObject();
            trips.put("trips", paths);
            res.put("status", "OK");
            res.put("data", trips);
            response = res.toString();
            r.sendResponseHeaders(200, response.length());
        }
        // Writing response body
        OutputStream os = r.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private void handleGETDriver(HttpExchange r) throws IOException, JSONException {
        String[] url = r.getRequestURI().getPath().split("/");
        String driver = url[url.length - 1];

        System.out.print("drier:" + driver);     //test
        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put("driver", driver);
        FindIterable<Document> cursor = collection.find(searchQuery);
        List<JSONObject> paths = new LinkedList<JSONObject>();
        for (Document document : cursor) {
            JSONObject path = new JSONObject();
            path.put("_id", document.get("_id"));
            path.put("distance", document.get("distance"));
            path.put("driverPayout", document.get("driverPayout"));
            path.put("startTime", document.get("startTime"));
            path.put("endTime", document.get("endTime"));
            path.put("timeElapsed", document.get("timeElapsed"));
            path.put("passenger", document.get("passenger"));
            paths.add(path);
        }

        JSONObject res = new JSONObject();
        if (paths.isEmpty()) {
            res.put("status", "NOT FOUND");
            res.put("data", new JSONObject());
            response = res.toString();
            r.sendResponseHeaders(404, response.length());
        } else {
            JSONObject trips = new JSONObject();
            trips.put("trips", paths);
            res.put("status", "OK");
            res.put("data", trips);
            response = res.toString();
            r.sendResponseHeaders(200, response.length());
        }
        // Writing response body
        OutputStream os = r.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private void handleGETDriverTime(HttpExchange r) throws JSONException, IOException, InterruptedException {
        String[] url = r.getRequestURI().getPath().split("/");
        String uid = url[url.length - 1];
        System.out.println("driverTrip: " + uid);     //test

        String driverId = "";
        String passengerId = "";
        System.out.println("bytes: " + uid.getBytes().length);
        JSONObject res = new JSONObject();
        if (uid.getBytes().length == 24) {
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("_id", new ObjectId(uid));
            FindIterable<Document> cursor = collection.find(searchQuery);
            if (cursor.iterator().hasNext()) {
                System.out.println("has next");     //test
                driverId = cursor.iterator().next().get("driver").toString();
                passengerId = cursor.iterator().next().get("passenger").toString();

                //Make request to location microservices to get arrival time
                int time = getArrivalTime(driverId, passengerId);

                if (time == -1) {
                    res.put("status", "NOT FOUND");
                    res.put("data", new JSONObject());
                    response = res.toString();
                    r.sendResponseHeaders(404, response.length());
                } else if (time == -2) {
                    res.put("status", "INTERNAL SERVER ERROR");
                    r.sendResponseHeaders(500, response.length());
                } else {
                    res.put("status", "OK");
                    JSONObject arrivalTime = new JSONObject();
                    arrivalTime.put("arrival_time", time);
                    res.put("data", arrivalTime);
                    response = res.toString();
                    r.sendResponseHeaders(200, response.length());
                }
            } else {
                res.put("status", "NOT FOUND");
                System.out.println("trip does not exist");
                res.put("data", new JSONObject());
                response = res.toString();
                r.sendResponseHeaders(404, response.length());
            }
        } else {
            res.put("status", "NOT FOUND");
            System.out.println("trip does not exist");
            res.put("data", new JSONObject());
            response = res.toString();
            r.sendResponseHeaders(404, response.length());
        }
        // Writing response body
        OutputStream os = r.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    public int getArrivalTime(String driverId, String passengerId) throws IOException, InterruptedException, JSONException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://locationmicroservice:8000/location/navigation/" + driverId + "?passengerUid=" + passengerId))
                .build();
        HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println("request sent: " + "http://locationmicroservice:8000/location/navigation/" + driverId + "?" + passengerId);   //testing
        System.out.println("response status: " + httpResponse.statusCode());
        if (httpResponse.statusCode() == 200) {
            System.out.println(httpResponse.body());
            JSONObject res = new JSONObject(httpResponse.body());
            JSONObject finalRes = (JSONObject) res.get("data");
            return finalRes.getInt("total_time");
        } else if (httpResponse.statusCode() == 404) {
            return -1;
        }
        return -2;
    }
}
