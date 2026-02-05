package com.jaydorah.glowingfriends.mixin;

import com.jaydorah.glowingfriends.util.GlowingManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void onIsGlowing(CallbackInfoReturnable<Boolean> info) {
        if ((Object) this instanceof PlayerEntity) {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity localPlayer = client.player;
            if (localPlayer != null && (Object) this != localPlayer) {
                PlayerEntity player = (PlayerEntity) (Object) this;
                UUID playerUUID = player.getUuid();
                if (GlowingManager.isPlayerGlowing(playerUUID)) {
                    info.setReturnValue(true);
                }
            }
        }
    }

    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void onGetTeamColorValue(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) (Object) this;
            if (GlowingManager.isPlayerGlowing(player.getUuid())) {
                cir.setReturnValue(GlowingManager.getGlowColor(player.getUuid()));
            }
        }
    }
}
