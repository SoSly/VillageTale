package org.sosly.villagetale.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class FakePlayer extends Player {

    public FakePlayer(ServerLevel level) {
        super(level, BlockPos.ZERO, 0.0F, new GameProfile(UUID.randomUUID(), "[VillageTale]"));
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isCreative() {
        return false;
    }

}
