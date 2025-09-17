package org.sosly.villageworks.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.UUID;

public class FakePlayer extends Player {
    private static final GameProfile FAKE_PROFILE = new GameProfile(UUID.fromString("00000000-0000-0000-0000-000000000000"), "[VillageWorks]");

    public FakePlayer(ServerLevel level) {
        super(level, BlockPos.ZERO, 0.0F, FAKE_PROFILE);
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