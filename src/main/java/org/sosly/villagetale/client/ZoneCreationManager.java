package org.sosly.villagetale.client;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.sosly.villagetale.api.IZoneShape;
import org.sosly.villagetale.client.gui.NewZoneScreen;
import org.sosly.villagetale.network.packets.serverbound.SetZoneCreationMode;
import org.sosly.villagetale.zone.shape.Box;

@OnlyIn(Dist.CLIENT)
public class ZoneCreationManager {
    private static ZoneCreationManager instance;

    private CreationMode mode = CreationMode.INACTIVE;
    private UUID villageId;
    private BlockPos startPos;

    private ZoneCreationManager() {
    }

    public static ZoneCreationManager getInstance() {
        if (instance == null) {
            instance = new ZoneCreationManager();
        }
        return instance;
    }

    public void startBoxCreation(UUID villageId) {
        this.mode = CreationMode.BOX_FIRST_CORNER;
        this.villageId = villageId;
        this.startPos = null;
        updatePlayerPersistentData(true);
    }

    public boolean handleClick(BlockPos pos) {
        return switch (mode) {
            case BOX_FIRST_CORNER, BOX_SECOND_CORNER -> handleBoxClick(pos);
            case INACTIVE -> false;
        };
    }

    private boolean handleBoxClick(BlockPos pos) {
        if (mode == CreationMode.BOX_FIRST_CORNER) {
            this.startPos = pos;
            this.mode = CreationMode.BOX_SECOND_CORNER;
            return true;
        }

        if (mode == CreationMode.BOX_SECOND_CORNER && startPos != null) {
            AABB bounds = new AABB(
                Math.min(startPos.getX(), pos.getX()),
                Math.min(startPos.getY(), pos.getY()),
                Math.min(startPos.getZ(), pos.getZ()),
                Math.max(startPos.getX(), pos.getX()) + 1,
                Math.max(startPos.getY(), pos.getY()) + 1,
                Math.max(startPos.getZ(), pos.getZ()) + 1
            );

            Box shape = new Box(bounds);

            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                mc.setScreen(new NewZoneScreen(villageId, shape));
            }

            return true;
        }

        return false;
    }

    public void cancel() {
        this.mode = CreationMode.INACTIVE;
        this.villageId = null;
        this.startPos = null;
        updatePlayerPersistentData(false);
    }

    private void updatePlayerPersistentData(boolean isActive) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getConnection() != null) {
            SetZoneCreationMode.send(isActive);
            System.out.println("[ZoneCreationManager] Sent zone creation mode packet: " + isActive);
        }
    }

    public boolean isActive() {
        return mode != CreationMode.INACTIVE;
    }

    public CreationMode getMode() {
        return mode;
    }

    public IZoneShape getPreviewShape(Vec3 cursorPos) {
        if (mode == CreationMode.BOX_SECOND_CORNER && startPos != null) {
            BlockPos cursor = BlockPos.containing(cursorPos);
            AABB bounds = new AABB(
                Math.min(startPos.getX(), cursor.getX()),
                Math.min(startPos.getY(), cursor.getY()),
                Math.min(startPos.getZ(), cursor.getZ()),
                Math.max(startPos.getX(), cursor.getX()) + 1,
                Math.max(startPos.getY(), cursor.getY()) + 1,
                Math.max(startPos.getZ(), cursor.getZ()) + 1
            );
            return new Box(bounds);
        }
        return null;
    }

    public enum CreationMode {
        INACTIVE,
        BOX_FIRST_CORNER,
        BOX_SECOND_CORNER
    }
}
