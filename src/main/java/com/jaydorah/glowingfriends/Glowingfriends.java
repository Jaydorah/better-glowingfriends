package com.jaydorah.glowingfriends;

import com.jaydorah.glowingfriends.util.GetUUID;
import com.jaydorah.glowingfriends.util.GlowingManager;
import com.jaydorah.glowingfriends.util.GlowingPlayer;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Glowingfriends implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("glowing-friends");

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> GlowingManager.saveGlowingPlayers());

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			LiteralArgumentBuilder<FabricClientCommandSource> command = ClientCommandManager.literal("glowingfriends");

			command.then(createSubCommand("ally", GlowingPlayer.Type.ALLY));
			command.then(createSubCommand("enemy", GlowingPlayer.Type.ENEMY));
			command.then(createSubCommand("friend", GlowingPlayer.Type.FRIEND));

			command.then(ClientCommandManager.literal("add")
					.then(ClientCommandManager.argument("player", StringArgumentType.string())
							.suggests(this::suggestOnlinePlayers)
							.executes(ctx -> addPlayer(ctx, GlowingPlayer.Type.NORMAL))));

			command.then(ClientCommandManager.literal("remove")
					.then(ClientCommandManager.argument("player", StringArgumentType.string())
							.suggests(this::suggestGlowingPlayers)
							.executes(this::removePlayer)));

			command.then(ClientCommandManager.literal("list")
					.executes(this::listPlayers));

			dispatcher.register(command);

			LiteralArgumentBuilder<FabricClientCommandSource> alias = ClientCommandManager.literal("gwf");
			alias.then(createSubCommand("ally", GlowingPlayer.Type.ALLY));
			alias.then(createSubCommand("enemy", GlowingPlayer.Type.ENEMY));
			alias.then(createSubCommand("friend", GlowingPlayer.Type.FRIEND));
			alias.then(ClientCommandManager.literal("add")
					.then(ClientCommandManager.argument("player", StringArgumentType.string())
							.suggests(this::suggestOnlinePlayers)
							.executes(ctx -> addPlayer(ctx, GlowingPlayer.Type.NORMAL))));
			alias.then(ClientCommandManager.literal("remove")
					.then(ClientCommandManager.argument("player", StringArgumentType.string())
							.suggests(this::suggestGlowingPlayers)
							.executes(this::removePlayer)));
			alias.then(ClientCommandManager.literal("list")
					.executes(this::listPlayers));
			dispatcher.register(alias);
		});
	}

	private LiteralArgumentBuilder<FabricClientCommandSource> createSubCommand(String name, GlowingPlayer.Type type) {
		return ClientCommandManager.literal(name)
				.then(ClientCommandManager.literal("add")
						.then(ClientCommandManager.argument("player", StringArgumentType.string())
								.suggests(this::suggestOnlinePlayers)
								.executes(ctx -> addPlayer(ctx, type))))
				.then(ClientCommandManager.literal("remove")
						.then(ClientCommandManager.argument("player", StringArgumentType.string())
								.suggests(this::suggestGlowingPlayers)
								.executes(this::removePlayer)))
				.then(ClientCommandManager.literal("list")
						.executes(this::listPlayers));
	}

	private int addPlayer(CommandContext<FabricClientCommandSource> context, GlowingPlayer.Type type) {
		String playerName = StringArgumentType.getString(context, "player");
		GetUUID.getUUIDAsync(playerName).thenAccept(uuidStr -> {
			if (uuidStr != null) {
				UUID uuid = UUID.fromString(uuidStr);
				GlowingManager.addGlowingPlayer(uuid, playerName, type);
				context.getSource().sendFeedback(Text.literal("Added ")
						.append(Text.literal(playerName).formatted(Formatting.GOLD))
						.append(" to " + type.name().toLowerCase() + " glowing friends."));
			} else {
				context.getSource().sendError(Text.literal("Could not find UUID for player: " + playerName));
			}
		});
		return 1;
	}

	private int removePlayer(CommandContext<FabricClientCommandSource> context) {
		String playerName = StringArgumentType.getString(context, "player");
		GlowingPlayer player = GlowingManager.getGlowingPlayerByName(playerName);
		if (player != null) {
			GlowingManager.removeGlowingPlayer(player.getUuid());
			context.getSource().sendFeedback(Text.literal("Removed ")
					.append(Text.literal(playerName).formatted(Formatting.GOLD))
					.append(" from glowing friends."));
		} else {
			context.getSource().sendError(Text.literal("Player " + playerName + " is not in the list."));
		}
		return 1;
	}

	private int listPlayers(CommandContext<FabricClientCommandSource> context) {
		Set<String> names = GlowingManager.getGlowingPlayerNames();
		if (names.isEmpty()) {
			context.getSource().sendFeedback(Text.literal("No glowing players added."));
		} else {
			context.getSource().sendFeedback(Text.literal("Glowing Players: ").formatted(Formatting.AQUA)
					.append(Text.literal(String.join(", ", names)).formatted(Formatting.WHITE)));
		}
		return 1;
	}

	private CompletableFuture<Suggestions> suggestOnlinePlayers(CommandContext<FabricClientCommandSource> context,
			SuggestionsBuilder builder) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world != null) {
			Set<UUID> glowingUUIDs = GlowingManager.getGlowingPlayerUUIDs();
			String input = builder.getRemaining().toLowerCase();
			for (PlayerEntity player : client.world.getPlayers()) {
				UUID playerUUID = player.getUuid();
				String playerName = player.getName().getString();
				if (!glowingUUIDs.contains(playerUUID) &&
						(client.player == null || !playerName.equals(client.player.getName().getString())) &&
						playerName.toLowerCase().startsWith(input)) {
					builder.suggest(playerName);
				}
			}
		}
		return builder.buildFuture();
	}

	private CompletableFuture<Suggestions> suggestGlowingPlayers(CommandContext<FabricClientCommandSource> context,
			SuggestionsBuilder builder) {
		Set<String> glowingPlayerNames = GlowingManager.getGlowingPlayerNames();
		String input = builder.getRemaining().toLowerCase();
		for (String playerName : glowingPlayerNames) {
			if (playerName.toLowerCase().startsWith(input)) {
				builder.suggest(playerName);
			}
		}
		return builder.buildFuture();
	}
}