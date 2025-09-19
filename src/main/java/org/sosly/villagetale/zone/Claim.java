package org.sosly.villagetale.zone;

import net.minecraft.nbt.CompoundTag;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class Claim {
    private final UUID villagerUUID;
    private final long expirationTime;

    public Claim(UUID villagerUUID, long expirationTime) {
        this.villagerUUID = Objects.requireNonNull(villagerUUID, "Villager UUID cannot be null");
        this.expirationTime = expirationTime;
    }

    public UUID getVillagerUUID() {
        return villagerUUID;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public boolean isExpired(long currentTime) {
        return currentTime >= expirationTime;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("VillagerUUID", villagerUUID.toString());
        tag.putLong("ExpirationTime", expirationTime);
        return tag;
    }

    public static Optional<Claim> deserializeNBT(CompoundTag tag) {
        if (!tag.contains("VillagerUUID") || !tag.contains("ExpirationTime")) {
            return Optional.empty();
        }

        try {
            UUID villagerUUID = UUID.fromString(tag.getString("VillagerUUID"));
            long expirationTime = tag.getLong("ExpirationTime");
            return Optional.of(new Claim(villagerUUID, expirationTime));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Claim claim = (Claim) obj;
        return expirationTime == claim.expirationTime && Objects.equals(villagerUUID, claim.villagerUUID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(villagerUUID, expirationTime);
    }

    @Override
    public String toString() {
        return "Claim{villagerUUID=" + villagerUUID + ", expirationTime=" + expirationTime + '}';
    }
}
