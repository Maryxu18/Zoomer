package ca.utoronto.utm.mcs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class API implements HttpHandler {
    public String requestBody;
    public String requestMethod;
    public String path;
    public String response;
    public HttpClient httpClient;
    public CompletableFuture<HttpResponse<String>> httpResponse;
    public HttpRequest httpRequest;

    public API() {
        httpClient = HttpClient.newBuilder().build();
    }

    @Override
    public void handle(HttpExchange r) throws IOException {
        requestMethod = r.getRequestMethod();
        requestBody = Utils.convert(r.getRequestBody());
        path = r.getRequestURI().toString();

        System.out.println("in API");   //testing
        System.out.println(path);   //testing

        try {
            if (path.startsWith("/user/")) {
                routeUserMicroservice(r);
            } else if (path.startsWith("/location/")) {
                routeLocationMicroservice(r);
            } else if (path.startsWith("/trip/")) {
                routeTripInfoMicroservice(r);
            } else {
                JSONObject res = new JSONObject();
                res.put("status", "BAD REQUEST");
                response = res.toString();
                r.sendResponseHeaders(400, response.length());
                OutputStream os = r.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    public void routeUserMicroservice(HttpExchange r) throws JSONException, IOException {
        try {
            if (requestMethod.equals("GET")) {
                handleGET("usermicroservice", r);
            } else if (requestMethod.equals("POST")) {
                handlePOST("usermicroservice", r);
            } else if (requestMethod.equals("PATCH")) {
                handlePATCH("usermicroservice", r);
            }
        } catch (Exception e) {
            System.out.println("Query failed");     //testing
            e.printStackTrace();                   //testing

            JSONObject res = new JSONObject();
            res.put("status", "BAD REQUEST");
            response = res.toString();
            r.sendResponseHeaders(400, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    public void routeLocationMicroservice(HttpExchange r) throws IOException, JSONException {
        try {
            if (requestMethod.equals("GET")) {
                handleGET("locationmicroservice", r);
            } else if (requestMethod.equals("POST")) {
                handlePOST("locationmicroservice", r);
            } else if (requestMethod.equals("PATCH")) {
                handlePATCH("locationmicroservice", r);
            } else if (requestMethod.equals("PUT")) {
                handlePUT("locationmicroservice", r);
            } else if (requestMethod.equals("DELETE")) {
                handleDELETE("locationmicroservice", r);
            }
        } catch (Exception e) {
            System.out.println("Query failed");     //testing
            e.printStackTrace();                   //testing

            JSONObject res = new JSONObject();
            res.put("status", "BAD REQUEST");
            response = res.toString();
            r.sendResponseHeaders(400, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    public void routeTripInfoMicroservice(HttpExchange r) throws IOException, JSONException {
        try {
            if (requestMethod.equals("GET")) {
                handleGET("tripinfomicroservice", r);
            } else if (requestMethod.equals("POST")) {
                handlePOST("tripinfomicroservice", r);
            } else if (requestMethod.equals("PATCH")) {
                handlePATCH("tripinfomicroservice", r);
            }
        } catch (Exception e) {
            System.out.println("Query failed");     //testing
            e.printStackTrace();                   //testing

            JSONObject res = new JSONObject();
            res.put("status", "BAD REQUEST");
            response = res.toString();
            r.sendResponseHeaders(400, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    public void handleGET(String microserviceName, HttpExchange r) throws JSONException, InterruptedException, IOException {
        httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://".concat(microserviceName).concat(":8000").concat(path)))
                .build();
        httpResponse = httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString());
        try {
            String responseBody = httpResponse.thenApply(HttpResponse::body).get(5, TimeUnit.SECONDS);
            int responseStatusCode = httpResponse.thenApply(HttpResponse::statusCode).get(5, TimeUnit.SECONDS);
            System.out.println("result of async body:" + responseBody);
            System.out.println("result of async status . :" + responseStatusCode);
            r.sendResponseHeaders(responseStatusCode, responseBody.length());
            OutputStream os = r.getResponseBody();
            os.write(responseBody.getBytes());
            os.close();
        } catch (Exception e) {
            JSONObject res = new JSONObject();
            System.out.print("in handle 400");
            res.put("status", "BAD REQUEST");
            response = res.toString();
            r.sendResponseHeaders(400, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    public void handlePOST(String microserviceName, HttpExchange r) throws IOException, InterruptedException, JSONException {
        httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://".concat(microserviceName).concat(":8000").concat(path)))
                .method("POST", HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
//        httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        httpResponse = httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString());
        try {
            String responseBody = httpResponse.thenApply(HttpResponse::body).get(5, TimeUnit.SECONDS);
            int responseStatusCode = httpResponse.thenApply(HttpResponse::statusCode).get(5, TimeUnit.SECONDS);
            System.out.println("result of async body:" + responseBody);
            System.out.println("result of async status . :" + responseStatusCode);
            r.sendResponseHeaders(responseStatusCode, responseBody.length());
            OutputStream os = r.getResponseBody();
            os.write(responseBody.getBytes());
            os.close();
        } catch (Exception e) {
            JSONObject res = new JSONObject();
            System.out.print("in handle 400");
            res.put("status", "BAD REQUEST");
            response = res.toString();
            r.sendResponseHeaders(400, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    public void handlePATCH(String microserviceName, HttpExchange r) throws IOException, InterruptedException, JSONException {
        httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://".concat(microserviceName).concat(":8000").concat(path)))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        httpResponse = httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString());
        try {
            String responseBody = httpResponse.thenApply(HttpResponse::body).get(5, TimeUnit.SECONDS);
            int responseStatusCode = httpResponse.thenApply(HttpResponse::statusCode).get(5, TimeUnit.SECONDS);
            System.out.println("result of async body:" + responseBody);
            System.out.println("result of async status . :" + responseStatusCode);
            r.sendResponseHeaders(responseStatusCode, responseBody.length());
            OutputStream os = r.getResponseBody();
            os.write(responseBody.getBytes());
            os.close();
        } catch (Exception e) {
            JSONObject res = new JSONObject();
            System.out.print("in handle 400");
            res.put("status", "BAD REQUEST");
            response = res.toString();
            r.sendResponseHeaders(400, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    public void handlePUT(String microserviceName, HttpExchange r) throws IOException, InterruptedException, JSONException {
        httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://".concat(microserviceName).concat(":8000").concat(path)))
                .method("PUT", HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        httpResponse = httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString());
        try {
            String responseBody = httpResponse.thenApply(HttpResponse::body).get(5, TimeUnit.SECONDS);
            int responseStatusCode = httpResponse.thenApply(HttpResponse::statusCode).get(5, TimeUnit.SECONDS);
            System.out.println("result of async body:" + responseBody);
            System.out.println("result of async status . :" + responseStatusCode);
            r.sendResponseHeaders(responseStatusCode, responseBody.length());
            OutputStream os = r.getResponseBody();
            os.write(responseBody.getBytes());
            os.close();
        } catch (Exception e) {
            JSONObject res = new JSONObject();
            System.out.print("in handle 400");
            res.put("status", "BAD REQUEST");
            response = res.toString();
            r.sendResponseHeaders(400, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    public void handleDELETE(String microserviceName, HttpExchange r) throws IOException, InterruptedException, JSONException {
        httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://".concat(microserviceName).concat(":8000").concat(path)))
                .method("DELETE", HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        httpResponse = httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString());
        try {
            String responseBody = httpResponse.thenApply(HttpResponse::body).get(5, TimeUnit.SECONDS);
            int responseStatusCode = httpResponse.thenApply(HttpResponse::statusCode).get(5, TimeUnit.SECONDS);
            System.out.println("result of async body:" + responseBody);
            System.out.println("result of async status . :" + responseStatusCode);
            r.sendResponseHeaders(responseStatusCode, responseBody.length());
            OutputStream os = r.getResponseBody();
            os.write(responseBody.getBytes());
            os.close();
        } catch (Exception e) {
            JSONObject res = new JSONObject();
            System.out.print("in handle 400");
            res.put("status", "BAD REQUEST");
            response = res.toString();
            r.sendResponseHeaders(400, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
