package com.jaydorah.glowingfriends.util;

import java.util.UUID;

public class GlowingPlayer {
    public enum Type {
        NORMAL(16777215),
        ALLY(16776960),
        ENEMY(16711680),
        FRIEND(65280);

        private final int color;

        Type(int color) {
            this.color = color;
        }

        public int getColor() {
            return color;
        }
    }

    private final UUID uuid;
    private final String name;
    private final Type type;

    public GlowingPlayer(UUID uuid, String name, Type type) {
        this.uuid = uuid;
        this.name = name;
        this.type = type;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public String getName() {
        return this.name;
    }

    public Type getType() {
        return type != null ? type : Type.NORMAL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GlowingPlayer that = (GlowingPlayer) o;
        return this.uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return this.uuid.hashCode();
    }
}
