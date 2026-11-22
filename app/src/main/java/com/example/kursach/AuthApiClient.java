package com.example.kursach;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class AuthApiClient {
    // Gradle installs an ADB reverse mapping from device localhost to the local API port.
    private static final String BASE_URL = "http://127.0.0.1:8080";

    private AuthApiClient() { }

    static Result postJson(String path, JSONObject payload) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(BASE_URL + path).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(15000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");
            try (OutputStream output = connection.getOutputStream()) {
                output.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }

            int statusCode = connection.getResponseCode();
            InputStream responseStream = statusCode >= 200 && statusCode < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            String responseBody = readBody(responseStream);
            JSONObject response = responseBody.isEmpty() ? new JSONObject() : new JSONObject(responseBody);

            if (statusCode >= 200 && statusCode < 300) {
                return Result.success(response.optLong("userId"), response.optString("username"));
            }
            String message = response.optString("message", "Не удалось выполнить запрос");
            return Result.failure(message);
        } catch (IOException | JSONException exception) {
            Log.e("AuthApiClient", "Request failed: " + path, exception);
            return Result.failure("Не удалось связаться с локальным сервером. Проверьте, что Docker запущен.");
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String readBody(InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
        }
        return body.toString();
    }

    static final class Result {
        final boolean successful;
        final long userId;
        final String username;
        final String message;

        private Result(boolean successful, long userId, String username, String message) {
            this.successful = successful;
            this.userId = userId;
            this.username = username;
            this.message = message;
        }

        static Result success(long userId, String username) {
            return new Result(true, userId, username, "");
        }

        static Result failure(String message) {
            return new Result(false, 0L, "", message);
        }
    }
}
