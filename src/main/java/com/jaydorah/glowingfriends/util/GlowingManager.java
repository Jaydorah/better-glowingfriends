package com.jaydorah.glowingfriends.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GlowingManager {
    private static final Set<GlowingPlayer> glowingPlayers = new HashSet<>();
    private static final Gson gson = new Gson();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(),
            "GlowingFriends.json");

    static {
        loadGlowingPlayers();
    }

    public static void addGlowingPlayer(UUID playerUUID, String playerName, GlowingPlayer.Type type) {
        glowingPlayers.removeIf(p -> p.getUuid().equals(playerUUID));
        glowingPlayers.add(new GlowingPlayer(playerUUID, playerName, type));
        saveGlowingPlayers();
    }

    public static void removeGlowingPlayer(UUID playerUUID) {
        glowingPlayers.removeIf(player -> player.getUuid().equals(playerUUID));
        saveGlowingPlayers();
    }

    public static boolean isPlayerGlowing(UUID playerUUID) {
        return glowingPlayers.stream().anyMatch(player -> player.getUuid().equals(playerUUID));
    }

    public static int getGlowColor(UUID playerUUID) {
        return glowingPlayers.stream()
                .filter(player -> player.getUuid().equals(playerUUID))
                .findFirst()
                .map(player -> player.getType().getColor())
                .orElse(16777215);
    }

    public static GlowingPlayer getGlowingPlayerByName(String playerName) {
        return glowingPlayers.stream().filter(player -> player.getName().equalsIgnoreCase(playerName)).findFirst()
                .orElse(null);
    }

    public static Set<String> getGlowingPlayerNames() {
        Set<String> names = new HashSet<>();
        for (GlowingPlayer player : glowingPlayers)
            names.add(player.getName());
        return names;
    }

    public static Set<UUID> getGlowingPlayerUUIDs() {
        Set<UUID> uuids = new HashSet<>();
        for (GlowingPlayer player : glowingPlayers)
            uuids.add(player.getUuid());
        return uuids;
    }

    public static void loadGlowingPlayers() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                Type type = new TypeToken<Set<GlowingPlayer>>() {
                }.getType();
                JsonElement jsonElement = gson.fromJson(reader, JsonElement.class);

                if (jsonElement == null || (jsonElement.isJsonArray() && jsonElement.getAsJsonArray().isEmpty())) {
                    return;
                }

                if (isOldFormat(jsonElement)) {
                    clearOldConfig();
                } else {
                    Set<GlowingPlayer> loadedPlayers = gson.fromJson(jsonElement, type);
                    if (loadedPlayers != null)
                        glowingPlayers.addAll(loadedPlayers);
                }
            } catch (IOException | com.google.gson.JsonSyntaxException e) {
                clearOldConfig();
            }
        }
    }

    public static void saveGlowingPlayers() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            gson.toJson(glowingPlayers, writer);
        } catch (IOException ignored) {
        }
    }

    private static boolean isOldFormat(JsonElement jsonElement) {
        if (jsonElement.isJsonArray()) {
            try {
                Set<String> oldFormatUUIDs = gson.fromJson(jsonElement, new TypeToken<Set<String>>() {
                }.getType());
                for (String uuid : oldFormatUUIDs) {
                    if (uuid == null)
                        continue;
                    UUID.fromString(uuid);
                }
                return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private static void clearOldConfig() {
        glowingPlayers.clear();
        saveGlowingPlayers();
    }

    private static void sendErrorToChat(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message).formatted(Formatting.RED), false);
        }
    }
}
