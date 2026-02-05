package com.jaydorah.glowingfriends.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GetUUID {
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Gson gson = new Gson();

    public static CompletableFuture<String> getUUIDAsync(String username) {
        return CompletableFuture.supplyAsync(() -> getUUID(username), executor);
    }

    private static String getUUID(String username) {
        try {
            String urlString = "https://api.mojang.com/users/profiles/minecraft/" + username;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null)
                        response.append(inputLine);

                    String jsonResponse = response.toString().trim();
                    JsonObject jsonObject = gson.fromJson(jsonResponse, JsonObject.class);
                    if (jsonObject.has("id")) {
                        String id = jsonObject.get("id").getAsString();
                        return formatUUID(id);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String formatUUID(String trimmedUUID) {
        if (trimmedUUID == null || trimmedUUID.length() != 32)
            return trimmedUUID;
        return trimmedUUID.replaceFirst(
                "([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{12})", "$1-$2-$3-$4-$5");
    }

    public static void shutdown() {
        executor.shutdown();
    }
}
