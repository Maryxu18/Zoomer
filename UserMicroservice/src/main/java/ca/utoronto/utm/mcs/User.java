package ca.utoronto.utm.mcs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.sql.*;
import java.util.Iterator;

public class User implements HttpHandler {
    public Connection connection;
    public String response;

    public User() throws ClassNotFoundException, SQLException {
      String url = "jdbc:postgresql://postgres:5432/root";
      Class.forName("org.postgresql.Driver");
      this.connection = DriverManager.getConnection(url, "root", "123456");
//        String url = "jdbc:postgresql://localhost:5432/root";
//        Class.forName("org.postgresql.Driver");
//        this.connection = DriverManager.getConnection(url, "postgres", "password");
    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void handle(HttpExchange r) throws IOException {
        System.out.println("in User microservice");
        try {
            if (r.getRequestMethod().equals("GET")) {
                handleGET(r);
            }
            if (r.getRequestMethod().equals("PATCH")) {
                handlePATCH(r);
            }
            if (r.getRequestMethod().equals("POST")) {
                String requestPath = r.getRequestURI().getPath();
                if (requestPath.equals("/user/register")) {
                    handlePOSTRegister(r);
                } else if (requestPath.equals("/user/login")) {
                    handlePOSTLogin(r);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handlePOSTRegister(HttpExchange r) throws IOException, JSONException {
        String requestBody = Utils.convert(r.getRequestBody());
        JSONObject res = new JSONObject();


        PreparedStatement ps;
        String prepare;
        ResultSet rs;
        try {
            JSONObject parsedInput = new JSONObject(requestBody);
            String name = parsedInput.getString("name");
            String email = parsedInput.getString("email");
            String password = parsedInput.getString("password");

            //Check if user exists already
            prepare = "SELECT uid FROM users WHERE email = ?";
            ps = this.connection.prepareStatement(prepare);
            ps.setString(1, email);

            System.out.println("Now executing the command: " + prepare.replaceAll("\\s+", " ") + "\n");

            rs = ps.executeQuery();
            if (rs.next()) {
                //user exists already so return 403 Forbidden
                res.put("status", "FORBIDDEN");
                this.response = res.toString();
                r.sendResponseHeaders(403, this.response.length());

            } else {
                prepare = "INSERT INTO users (uid, email, prefer_name, password, rides, isDriver, availableCoupons, redeemedCoupons)" +
                        " VALUES (DEFAULT,?,?,?,0,DEFAULT,array[]::integer[], array[]::integer[])";
                ps = this.connection.prepareStatement(prepare);
                ps.setString(1, email);
                ps.setString(2, name);
                ps.setString(3, String.valueOf(password.hashCode()));
                System.out.println("Now executing the command: " + prepare.replaceAll("\\s+", " ") + "\n");

                ps.executeUpdate();
                ps.close();
                rs.close();

                //send OK status
                res.put("status", "OK");
                this.response = res.toString();
                r.sendResponseHeaders(200, this.response.length());

            }
        } catch (SQLException se) {
            res.put("status", "INTERNAL SERVER ERROR");
            this.response = res.toString();
            r.sendResponseHeaders(500, this.response.length());

        } catch (JSONException se) {
            res.put("status", "BAD REQUEST");
            this.response = res.toString();
            r.sendResponseHeaders(400, this.response.length());
        } finally {
            // Writing response body
            OutputStream os = r.getResponseBody();
            os.write(this.response.getBytes());
            os.close();
        }
    }

    public void handlePOSTLogin(HttpExchange r) throws IOException, JSONException {
        String requestBody = Utils.convert(r.getRequestBody());
        JSONObject res = new JSONObject();
        PreparedStatement ps;
        String prepare;
        ResultSet rs;

        try {
            JSONObject parsedInput = new JSONObject(requestBody);
            String email = parsedInput.getString("email");
            String password = parsedInput.getString("password");

            //Get user
            prepare = "SELECT uid FROM users WHERE email = ? AND password = ?";
            ps = this.connection.prepareStatement(prepare);
            ps.setString(1, email);
            ps.setString(2, String.valueOf(password.hashCode()));
            System.out.println("Now executing the command: " + prepare.replaceAll("\\s+", " ") + "\n");

            rs = ps.executeQuery();

            if (rs.next()) {
                //return OK status if credentials match
                res.put("status", "OK");
                this.response = res.toString();
                r.sendResponseHeaders(200, this.response.length());

            } else {
                //send OK status
                res.put("status", "UNAUTHORIZED");
                this.response = res.toString();
                r.sendResponseHeaders(401, this.response.length());
            }
            ps.close();
            rs.close();
        } catch (SQLException se) {
            res.put("status", "INTERNAL SERVER ERROR");
            this.response = res.toString();
            r.sendResponseHeaders(500, this.response.length());
        } catch (JSONException se) {
            res.put("status", "BAD REQUEST");
            this.response = res.toString();
            r.sendResponseHeaders(400, this.response.length());
        } finally {
            // Writing response body
            OutputStream os = r.getResponseBody();
            os.write(this.response.getBytes());
            os.close();
        }
    }

    public void handleGET(HttpExchange r) throws IOException, JSONException, SQLException {
        String[] url = r.getRequestURI().getPath().split("/");
        System.out.println(url[url.length - 1]);
        if (isNumeric(url[url.length - 1])) {
            try {
                ResultSet rs;
                int uid = Integer.parseInt(url[url.length - 1]);
                String prepare = "SELECT prefer_name as name, email, rides, isdriver,availableCoupons, redeemedCoupons FROM users WHERE uid = ?";
                PreparedStatement ps = this.connection.prepareStatement(prepare);
                ps.setInt(1, uid);
                rs = ps.executeQuery();
                if (rs.next()) {
                    JSONObject var = new JSONObject();
                    String name = rs.getString("name");
                    String email = rs.getString("email");
                    String rides = rs.getString("rides");
                    Boolean isDriver = rs.getBoolean("isdriver");
                    Array availableCoupons = rs.getArray("availableCoupons");
                    Array redeemedCoupons = rs.getArray("redeemedCoupons");
                    var.put("name", name);
                    var.put("email", email);
                    var.put("rides", rides);
                    var.put("is_driver", isDriver);
                    var.put("availableCoupons", availableCoupons.toString());
                    var.put("redeemedCoupons", redeemedCoupons.toString());
                    String response = var.toString();
                    r.sendResponseHeaders(200, response.length());
                    // Writing response body
                    OutputStream os = r.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } else {
                    JSONObject res = new JSONObject();
                    res.put("status", "NOT FOUND");
                    String response = res.toString();
                    r.sendResponseHeaders(404, response.length());
                    // Writing response body
                    OutputStream os = r.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }

            } catch (SQLException se) {
                JSONObject res = new JSONObject();
                res.put("status", "INTERNAL SERVER ERROR");
                String response = res.toString();
                r.sendResponseHeaders(500, response.length());
                // Writing response body
                OutputStream os = r.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }

    }

    public void handlePATCH(HttpExchange r) throws IOException, JSONException, SQLException {
        String[] url = r.getRequestURI().getPath().split("/");
        if (isNumeric(url[url.length - 1])) {
            ResultSet rs;
            int uid = Integer.parseInt(url[url.length - 1]);
            String preCheck = "SELECT count(*) as c FROM users WHERE uid = ?";
            PreparedStatement ps = this.connection.prepareStatement(preCheck);
            ps.setInt(1, uid);
            try {
                rs = ps.executeQuery();
                if (rs.next()) {
                    int numOfUser = rs.getInt("c");
                    String alters = "";
                    if (numOfUser == 1) {
                        // Packing the alter info
                        String body = Utils.convert(r.getRequestBody());
                        JSONObject deserialized = new JSONObject(body);
                        Iterator<?> it = deserialized.keys();
                        String[] alternates = new String[deserialized.length()];
                        int order = 0;
                        System.out.println(deserialized.length());
                        while (it.hasNext()) {
                            System.out.println(order);
                            String key = it.next().toString();
                            if (order != 0 && order < deserialized.length()) {
                                alters += ", ";
                            }
                            if (key.equals("is_driver")) {
                                alternates[order] = "is_driver";
                                alters += "isdriver = ? ";
                            } else if (key.equals("name")) {
                                alternates[order] = "name";
                                alters += "prefer_name = ? ";
                            } else {
                                alternates[order] = key;
                                alters += key + " = ? ";
                            }
                            order++;
                        }
                        String alternation = "UPDATE users SET " + alters + " WHERE uid = ?";
                        PreparedStatement ps1 = this.connection.prepareStatement(alternation);
                        for (int j = 0; j < deserialized.length(); j++) {
                            System.out.println(alternates[j]);
                            System.out.print(j);
                            if (alternates[j].equals("is_driver")) {
                                System.out.println(deserialized.getBoolean(alternates[j]));
                                ps1.setBoolean(j + 1, deserialized.getBoolean(alternates[j]));
                            } else if (alternates[j].equals("rides")) {
                                ps1.setInt(j + 1, deserialized.getInt(alternates[j]));
                            } else {
                                ps1.setString(j + 1, deserialized.getString(alternates[j]));
                            }
                        }
                        ps1.setInt(deserialized.length() + 1, uid);
                        try {
                            ps1.executeUpdate();
                            // success
                            JSONObject res = new JSONObject();
                            res.put("status", "OK");
                            String response = res.toString();
                            r.sendResponseHeaders(200, response.length());
                            // Writing response body
                            OutputStream os = r.getResponseBody();
                            os.write(response.getBytes());
                            os.close();
                        } catch (SQLException e) {
                            JSONObject res = new JSONObject();
                            res.put("status", "INTERNAL SERVER ERROR");
                            String response = res.toString();
                            r.sendResponseHeaders(500, response.length());
                            // Writing response body
                            OutputStream os = r.getResponseBody();
                            os.write(response.getBytes());
                            os.close();
                        }
                    }
                } else {
                    JSONObject res = new JSONObject();
                    res.put("status", "NOT FOUND");
                    String response = res.toString();
                    r.sendResponseHeaders(404, response.length());
                    // Writing response body
                    OutputStream os = r.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
            } catch (SQLException e) {
                JSONObject res = new JSONObject();
                res.put("status", "INTERNAL SERVER ERROR");
                String response = res.toString();
                r.sendResponseHeaders(500, response.length());
                // Writing response body
                OutputStream os = r.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        } else {
            JSONObject res = new JSONObject();
            res.put("status", "BAD REQUEST");
            String response = res.toString();
            r.sendResponseHeaders(400, response.length());
            // Writing response body
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
