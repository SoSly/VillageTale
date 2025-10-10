package org.sosly.villagetale.client;

import java.util.ArrayList;
import java.util.List;
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
import org.sosly.villagetale.zone.shape.Cylinder;
import org.sosly.villagetale.zone.shape.Point;
import org.sosly.villagetale.zone.shape.Route;

@OnlyIn(Dist.CLIENT)
public class ZoneCreationManager {
    private static ZoneCreationManager instance;

    private CreationMode mode = CreationMode.INACTIVE;
    private UUID villageId;
    private BlockPos startPos;
    private List<BlockPos> routeWaypoints = new ArrayList<>();

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

    public void startPointCreation(UUID villageId) {
        this.mode = CreationMode.POINT;
        this.villageId = villageId;
        this.startPos = null;
        updatePlayerPersistentData(true);
    }

    public void startCylinderCreation(UUID villageId) {
        this.mode = CreationMode.CYLINDER_CENTER;
        this.villageId = villageId;
        this.startPos = null;
        updatePlayerPersistentData(true);
    }

    public void startRouteCreation(UUID villageId) {
        this.mode = CreationMode.ROUTE_WAYPOINTS;
        this.villageId = villageId;
        this.startPos = null;
        this.routeWaypoints.clear();
        updatePlayerPersistentData(true);
    }

    public boolean handleClick(BlockPos pos) {
        return switch (mode) {
            case BOX_FIRST_CORNER, BOX_SECOND_CORNER -> handleBoxClick(pos);
            case POINT -> handlePointClick(pos);
            case CYLINDER_CENTER, CYLINDER_DIMENSIONS -> handleCylinderClick(pos);
            case ROUTE_WAYPOINTS -> handleRouteClick(pos);
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

    private boolean handlePointClick(BlockPos pos) {
        Point shape = new Point(pos);

        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            mc.setScreen(new NewZoneScreen(villageId, shape));
        }

        return true;
    }

    private boolean handleCylinderClick(BlockPos pos) {
        if (mode == CreationMode.CYLINDER_CENTER) {
            this.startPos = pos;
            this.mode = CreationMode.CYLINDER_DIMENSIONS;
            return true;
        }

        if (mode == CreationMode.CYLINDER_DIMENSIONS && startPos != null) {
            double dx = pos.getX() - startPos.getX();
            double dz = pos.getZ() - startPos.getZ();
            int radius = (int) Math.ceil(Math.sqrt(dx * dx + dz * dz));
            int height = Math.abs(pos.getY() - startPos.getY()) + 1;

            if (radius < 1) {
                radius = 1;
            }
            if (height < 1) {
                height = 1;
            }

            int minY = Math.min(startPos.getY(), pos.getY());
            BlockPos baseCenter = new BlockPos(startPos.getX(), minY, startPos.getZ());
            Cylinder shape = new Cylinder(baseCenter, radius, height);

            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                mc.setScreen(new NewZoneScreen(villageId, shape));
            }

            return true;
        }

        return false;
    }

    private boolean handleRouteClick(BlockPos pos) {
        routeWaypoints.add(pos);
        return true;
    }

    public void finalizeRoute() {
        if (mode != CreationMode.ROUTE_WAYPOINTS) {
            return;
        }

        if (routeWaypoints.isEmpty()) {
            cancel();
            return;
        }

        Route shape = new Route();
        for (BlockPos waypoint : routeWaypoints) {
            shape.addPoint(waypoint);
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            mc.setScreen(new NewZoneScreen(villageId, shape));
        }
    }

    public void cancel() {
        this.mode = CreationMode.INACTIVE;
        this.villageId = null;
        this.startPos = null;
        this.routeWaypoints.clear();
        updatePlayerPersistentData(false);
    }

    private void updatePlayerPersistentData(boolean isActive) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getConnection() != null) {
            SetZoneCreationMode.send(isActive);
        }
    }

    public boolean isActive() {
        return mode != CreationMode.INACTIVE;
    }

    public CreationMode getMode() {
        return mode;
    }

    public IZoneShape getPreviewShape(Vec3 cursorPos) {
        return switch (mode) {
            case BOX_SECOND_CORNER -> getBoxPreview(cursorPos);
            case POINT -> getPointPreview(cursorPos);
            case CYLINDER_DIMENSIONS -> getCylinderPreview(cursorPos);
            case ROUTE_WAYPOINTS -> getRoutePreview(cursorPos);
            default -> null;
        };
    }

    private IZoneShape getBoxPreview(Vec3 cursorPos) {
        if (startPos == null) {
            return null;
        }

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

    private IZoneShape getPointPreview(Vec3 cursorPos) {
        BlockPos pos = BlockPos.containing(cursorPos);
        return new Point(pos);
    }

    private IZoneShape getCylinderPreview(Vec3 cursorPos) {
        if (startPos == null) {
            return null;
        }

        BlockPos cursor = BlockPos.containing(cursorPos);
        double dx = cursor.getX() - startPos.getX();
        double dz = cursor.getZ() - startPos.getZ();
        int radius = (int) Math.ceil(Math.sqrt(dx * dx + dz * dz));
        int height = Math.abs(cursor.getY() - startPos.getY()) + 1;

        if (radius < 1) {
            radius = 1;
        }
        if (height < 1) {
            height = 1;
        }

        int minY = Math.min(startPos.getY(), cursor.getY());
        BlockPos baseCenter = new BlockPos(startPos.getX(), minY, startPos.getZ());
        return new Cylinder(baseCenter, radius, height);
    }

    private IZoneShape getRoutePreview(Vec3 cursorPos) {
        Route route = new Route();
        for (BlockPos waypoint : routeWaypoints) {
            route.addPoint(waypoint);
        }

        BlockPos cursor = BlockPos.containing(cursorPos);
        route.addPoint(cursor);

        return route;
    }

    public enum CreationMode {
        INACTIVE,
        BOX_FIRST_CORNER,
        BOX_SECOND_CORNER,
        POINT,
        CYLINDER_CENTER,
        CYLINDER_DIMENSIONS,
        ROUTE_WAYPOINTS
    }
}
