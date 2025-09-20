package org.sosly.villagetale.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.IZoneShape;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.command.arguments.VillageUUIDArgument;
import org.sosly.villagetale.command.arguments.ZoneUUIDArgument;
import org.sosly.villagetale.data.VillageInfo;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.zone.Zone;
import org.sosly.villagetale.zone.shape.Point;
import org.sosly.villagetale.zone.shape.Rectangle;
import org.sosly.villagetale.zone.shape.Route;
import org.sosly.villagetale.zone.shape.Sphere;
import org.sosly.villagetale.zone.type.TownHall;
import org.sosly.villagetale.zone.ZoneRegistry;

public class ZoneCommand {
    private static final SuggestionProvider<CommandSourceStack> ZONE_TYPE_SUGGESTIONS =
        (context, builder) -> SharedSuggestionProvider.suggestResource(
            StreamSupport.stream(ZoneRegistry.INSTANCE.getZoneTypeIDs().spliterator(), false),
            builder
        );

    private static IVillageCapability getVillageCapability(CommandSourceStack source, UUID villageId) {
        ServerLevel level = source.getLevel();

        IVillagesCapability villages = level.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
        if (villages == null) {
            source.sendFailure(Component.literal("Villages capability not found"));
            return null;
        }

        VillageInfo village = villages.getVillageById(villageId);
        if (village == null) {
            source.sendFailure(Component.literal("Village not found"));
            return null;
        }

        ChunkPos villageChunk = village.getVillageStartingChunk();
        LevelChunk chunk = level.getChunk(villageChunk.x, villageChunk.z);
        IVillageCapability villageCapability = chunk.getCapability(Capabilities.VILLAGE_CAPABILITY).orElse(null);
        if (villageCapability == null) {
            source.sendFailure(Component.literal("Village capability not found"));
            return null;
        }

        return villageCapability;
    }

    public static void register(LiteralArgumentBuilder<CommandSourceStack> parentCommand) {
        parentCommand.then(Commands.literal("zone")
            .then(Commands.argument("villageUUID", VillageUUIDArgument.villageUUID())
                .suggests((context, builder) -> VillageUUIDArgument.suggest(context, builder))
                .then(Commands.literal("create")
                    .then(Commands.literal("rectangle")
                        .then(Commands.argument("pos1", BlockPosArgument.blockPos())
                            .then(Commands.argument("pos2", BlockPosArgument.blockPos())
                                .then(Commands.argument("type", ResourceLocationArgument.id())
                                    .suggests(ZONE_TYPE_SUGGESTIONS)
                                    .executes(ZoneCommand::createRectangleZone)
                                    .then(Commands.argument("name", StringArgumentType.string())
                                        .executes(ZoneCommand::createRectangleZoneWithName))))))
                    .then(Commands.literal("sphere")
                        .then(Commands.argument("center", BlockPosArgument.blockPos())
                            .then(Commands.argument("radius", IntegerArgumentType.integer(1))
                                .then(Commands.argument("type", ResourceLocationArgument.id())
                                    .suggests(ZONE_TYPE_SUGGESTIONS)
                                    .executes(ZoneCommand::createSphereZone)
                                    .then(Commands.argument("name", StringArgumentType.string())
                                        .executes(ZoneCommand::createSphereZoneWithName))))))
                    .then(Commands.literal("point")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                            .then(Commands.argument("type", ResourceLocationArgument.id())
                                .suggests(ZONE_TYPE_SUGGESTIONS)
                                .executes(ZoneCommand::createPointZone)
                                .then(Commands.argument("name", StringArgumentType.string())
                                    .executes(ZoneCommand::createPointZoneWithName)))))
                    .then(Commands.literal("route")
                        .then(Commands.argument("type", ResourceLocationArgument.id())
                            .suggests(ZONE_TYPE_SUGGESTIONS)
                            .executes(ZoneCommand::createRouteZone)
                            .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ZoneCommand::createRouteZoneWithName)))))
                .then(Commands.literal("delete")
                    .then(Commands.argument("zoneUUID", ZoneUUIDArgument.zoneUUID())
                        .suggests((context, builder) -> ZoneUUIDArgument.suggest(context, builder))
                        .executes(ZoneCommand::deleteZone)))
                .then(Commands.literal("list")
                    .executes(ZoneCommand::listZones))
                .then(Commands.literal("info")
                    .then(Commands.argument("zoneUUID", ZoneUUIDArgument.zoneUUID())
                        .suggests((context, builder) -> ZoneUUIDArgument.suggest(context, builder))
                        .executes(ZoneCommand::zoneInfo)))
                .then(Commands.literal("route")
                    .then(Commands.argument("zoneUUID", ZoneUUIDArgument.zoneUUID())
                        .suggests((context, builder) -> ZoneUUIDArgument.suggest(context, builder))
                        .then(Commands.literal("add")
                            .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(ZoneCommand::addRoutePoint)))
                        .then(Commands.literal("clear")
                            .executes(ZoneCommand::clearRoute))))
                .then(Commands.literal("assign")
                    .then(Commands.argument("zoneUUID", ZoneUUIDArgument.zoneUUID())
                        .suggests((context, builder) -> ZoneUUIDArgument.suggest(context, builder))
                        .then(Commands.argument("villager", EntityArgument.entity())
                            .executes(ZoneCommand::assignVillager))))
                .then(Commands.literal("unassign")
                    .then(Commands.argument("villager", EntityArgument.entity())
                        .executes(ZoneCommand::unassignVillager)))));
    }

    private static int createRectangleZone(CommandContext<CommandSourceStack> context) {
        return createRectangleZoneInternal(context, null);
    }

    private static int createRectangleZoneWithName(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        return createRectangleZoneInternal(context, name);
    }

    private static int createRectangleZoneInternal(CommandContext<CommandSourceStack> context, String name) {
        try {
            CommandSourceStack source = context.getSource();
            UUID villageId = VillageUUIDArgument.getVillageUUID(context, "villageUUID");
            ResourceLocation zoneType = ResourceLocationArgument.getId(context, "type");

            if (zoneType.equals(TownHall.ID)) {
                source.sendFailure(Component.literal("TOWNHALL zones are automatically managed and cannot be created manually"));
                return 0;
            }

            BlockPos pos1 = BlockPosArgument.getBlockPos(context, "pos1");
            BlockPos pos2 = BlockPosArgument.getBlockPos(context, "pos2");

            AABB bounds = new AABB(
                Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()),
                Math.max(pos1.getX(), pos2.getX()) + 1, Math.max(pos1.getY(), pos2.getY()) + 1, Math.max(pos1.getZ(), pos2.getZ()) + 1
            );

            IVillageCapability capability = getVillageCapability(source, villageId);
            if (capability == null) return 0;

            Zone zone = Rectangle.builder(source.getLevel(), capability, capability.getZones().size())
                .setBounds(bounds)
                .setType(zoneType)
                .build();

            if (name != null) {
                zone.setName(name);
            }

            capability.addZone(zone);
            source.sendSuccess(() -> Component.literal("Created rectangle zone: " + zone.getName()), true);
            return 1;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to create rectangle zone: " + e.getMessage()));
            return 0;
        }
    }

    private static int createSphereZone(CommandContext<CommandSourceStack> context) {
        return createSphereZoneInternal(context, null);
    }

    private static int createSphereZoneWithName(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        return createSphereZoneInternal(context, name);
    }

    private static int createSphereZoneInternal(CommandContext<CommandSourceStack> context, String name) {
        try {
            CommandSourceStack source = context.getSource();
            UUID villageId = VillageUUIDArgument.getVillageUUID(context, "villageUUID");
            ResourceLocation zoneType = ResourceLocationArgument.getId(context, "type");

            if (zoneType.equals(TownHall.ID)) {
                source.sendFailure(Component.literal("TOWNHALL zones are automatically managed and cannot be created manually"));
                return 0;
            }

            BlockPos center = BlockPosArgument.getBlockPos(context, "center");
            int radius = IntegerArgumentType.getInteger(context, "radius");

            IVillageCapability capability = getVillageCapability(source, villageId);
            if (capability == null) return 0;

            Zone zone = Sphere.builder(source.getLevel(), capability, capability.getZones().size())
                .setCenter(center)
                .setRadius(radius)
                .setType(zoneType)
                .build();

            if (name != null) {
                zone.setName(name);
            }

            capability.addZone(zone);
            source.sendSuccess(() -> Component.literal("Created sphere zone: " + zone.getName()), true);
            return 1;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to create sphere zone: " + e.getMessage()));
            return 0;
        }
    }

    private static int createPointZone(CommandContext<CommandSourceStack> context) {
        return createPointZoneInternal(context, null);
    }

    private static int createPointZoneWithName(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        return createPointZoneInternal(context, name);
    }

    private static int createPointZoneInternal(CommandContext<CommandSourceStack> context, String name) {
        try {
            CommandSourceStack source = context.getSource();
            UUID villageId = VillageUUIDArgument.getVillageUUID(context, "villageUUID");
            ResourceLocation zoneType = ResourceLocationArgument.getId(context, "type");

            if (zoneType.equals(TownHall.ID)) {
                source.sendFailure(Component.literal("TOWNHALL zones are automatically managed and cannot be created manually"));
                return 0;
            }

            BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");

            IVillageCapability capability = getVillageCapability(source, villageId);
            if (capability == null) return 0;

            Zone zone = Point.builder(source.getLevel(), capability, capability.getZones().size())
                .setPos(pos)
                .setType(zoneType)
                .build();

            if (name != null) {
                zone.setName(name);
            }

            capability.addZone(zone);
            source.sendSuccess(() -> Component.literal("Created point zone: " + zone.getName()), true);
            return 1;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to create point zone: " + e.getMessage()));
            return 0;
        }
    }

    private static int createRouteZone(CommandContext<CommandSourceStack> context) {
        return createRouteZoneInternal(context, null);
    }

    private static int createRouteZoneWithName(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        return createRouteZoneInternal(context, name);
    }

    private static int createRouteZoneInternal(CommandContext<CommandSourceStack> context, String name) {
        try {
            CommandSourceStack source = context.getSource();
            UUID villageId = VillageUUIDArgument.getVillageUUID(context, "villageUUID");
            ResourceLocation zoneType = ResourceLocationArgument.getId(context, "type");

            if (zoneType.equals(TownHall.ID)) {
                source.sendFailure(Component.literal("TOWNHALL zones are automatically managed and cannot be created manually"));
                return 0;
            }

            IVillageCapability capability = getVillageCapability(source, villageId);
            if (capability == null) return 0;

            Zone zone = Route.builder(source.getLevel(), capability, capability.getZones().size())
                .setType(zoneType)
                .build();

            if (name != null) {
                zone.setName(name);
            }

            capability.addZone(zone);
            source.sendSuccess(() -> Component.literal("Created route zone: " + zone.getName() + " (add points with /vt zone route add)"), true);
            return 1;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to create route zone: " + e.getMessage()));
            return 0;
        }
    }

    private static int deleteZone(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            UUID villageId = VillageUUIDArgument.getVillageUUID(context, "villageUUID");
            UUID zoneId = ZoneUUIDArgument.getZoneUUID(context, "zoneUUID");

            IVillageCapability capability = getVillageCapability(source, villageId);
            if (capability == null) return 0;

            List<IVillageZone> zones = capability.getZones();
            IVillageZone toRemove = zones.stream().filter(zone -> zone.getUUID().equals(zoneId)).findFirst().orElse(null);

            if (toRemove == null) {
                source.sendFailure(Component.literal("Zone not found"));
                return 0;
            }

            if (toRemove.getType().getID().equals(TownHall.ID)) {
                source.sendFailure(Component.literal("Cannot delete TOWNHALL zones"));
                return 0;
            }

            zones.remove(toRemove);
            source.sendSuccess(() -> Component.literal("Deleted zone: " + toRemove.getName()), true);
            return 1;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to delete zone: " + e.getMessage()));
            return 0;
        }
    }

    private static int listZones(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            UUID villageId = VillageUUIDArgument.getVillageUUID(context, "villageUUID");

            IVillageCapability capability = getVillageCapability(source, villageId);
            if (capability == null) return 0;

            List<IVillageZone> zones = capability.getZones();
            if (zones.isEmpty()) {
                source.sendSuccess(() -> Component.literal("No zones in this village"), false);
                return 1;
            }

            source.sendSuccess(() -> Component.literal("Zones in village:"), false);
            for (IVillageZone zone : zones) {
                source.sendSuccess(() ->
                    Component.literal("- " + zone.getName() +
                        " (Type: " + zone.getType().getID() +
                        ", ID: " + zone.getUUID() + ")"), false);
            }
            return 1;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to list zones: " + e.getMessage()));
            return 0;
        }
    }

    private static int zoneInfo(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            UUID villageId = VillageUUIDArgument.getVillageUUID(context, "villageUUID");
            UUID zoneId = ZoneUUIDArgument.getZoneUUID(context, "zoneUUID");

            IVillageCapability capability = getVillageCapability(source, villageId);
            if (capability == null) return 0;

            IVillageZone zone = capability.getZones().stream().filter(z -> z.getUUID().equals(zoneId)).findFirst().orElse(null);
            if (zone == null) {
                source.sendFailure(Component.literal("Zone not found"));
                return 0;
            }

            source.sendSuccess(() -> Component.literal("=== Zone Info ==="), false);
            source.sendSuccess(() -> Component.literal("Name: " + zone.getName()), false);
            source.sendSuccess(() -> Component.literal("UUID: " + zone.getUUID()), false);
            source.sendSuccess(() -> Component.literal("Type: " + zone.getType().getID()), false);

            if (zone instanceof Zone zImpl) {
                IZoneShape shape = zImpl.getShape();
                if (shape instanceof Rectangle rect) {
                    AABB bounds = rect.getBounds();
                    source.sendSuccess(() ->
                        Component.literal("Shape: Rectangle (" +
                            (int)bounds.minX + "," + (int)bounds.minY + "," + (int)bounds.minZ + " to " +
                            (int)(bounds.maxX-1) + "," + (int)(bounds.maxY-1) + "," + (int)(bounds.maxZ-1) + ")"), false);
                } else if (shape instanceof Sphere sphere) {
                    source.sendSuccess(() ->
                        Component.literal("Shape: Sphere (center: " + sphere.getCenter().toShortString() +
                            ", radius: " + sphere.getRadius() + ")"), false);
                } else if (shape instanceof Point point) {
                    source.sendSuccess(() ->
                        Component.literal("Shape: Point (" + point.getPos().toShortString() + ")"), false);
                } else if (shape instanceof Route route) {
                    List<BlockPos> path = route.getPath();
                    source.sendSuccess(() ->
                        Component.literal("Shape: Route (" + path.size() + " points)"), false);
                }
            }

            List<UUID> assigned = zone.getAssignedVillagers();
            source.sendSuccess(() ->
                Component.literal("Assigned Villagers: " + assigned.size()), false);

            return 1;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to get zone info: " + e.getMessage()));
            return 0;
        }
    }

    private static int addRoutePoint(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            UUID villageId = VillageUUIDArgument.getVillageUUID(context, "villageUUID");
            UUID zoneId = ZoneUUIDArgument.getZoneUUID(context, "zoneUUID");
            BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");

            IVillageCapability capability = getVillageCapability(source, villageId);
            if (capability == null) return 0;

            for (IVillageZone zone : capability.getZones()) {
                if (zone.getUUID().equals(zoneId)) {
                    if (zone instanceof Zone zImpl && zImpl.getShape() instanceof Route route) {
                        route.addPoint(pos);
                        source.sendSuccess(() ->
                            Component.literal("Added point to route: " + pos.toShortString()), true);
                        return 1;
                    } else {
                        source.sendFailure(Component.literal("Zone is not a route type"));
                        return 0;
                    }
                }
            }

            source.sendFailure(Component.literal("Zone not found"));
            return 0;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to add route point: " + e.getMessage()));
            return 0;
        }
    }

    private static int clearRoute(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            UUID villageId = VillageUUIDArgument.getVillageUUID(context, "villageUUID");
            UUID zoneId = ZoneUUIDArgument.getZoneUUID(context, "zoneUUID");

            IVillageCapability capability = getVillageCapability(source, villageId);
            if (capability == null) return 0;

            for (IVillageZone zone : capability.getZones()) {
                if (zone.getUUID().equals(zoneId)) {
                    if (zone instanceof Zone zImpl && zImpl.getShape() instanceof Route route) {
                        route.clearPath();
                        source.sendSuccess(() ->
                            Component.literal("Cleared route points"), true);
                        return 1;
                    } else {
                        source.sendFailure(Component.literal("Zone is not a route type"));
                        return 0;
                    }
                }
            }

            source.sendFailure(Component.literal("Zone not found"));
            return 0;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to clear route: " + e.getMessage()));
            return 0;
        }
    }

    private static int assignVillager(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            UUID villageId = VillageUUIDArgument.getVillageUUID(context, "villageUUID");
            UUID zoneId = ZoneUUIDArgument.getZoneUUID(context, "zoneUUID");
            Entity entity = EntityArgument.getEntity(context, "villager");

            if (!(entity instanceof Villager villager)) {
                source.sendFailure(Component.literal("Entity is not a villager"));
                return 0;
            }

            IVillageCapability capability = getVillageCapability(source, villageId);
            if (capability == null) return 0;

            for (IVillageZone zone : capability.getZones()) {
                if (zone.getUUID().equals(zoneId)) {
                    zone.addAssignedVillager(villager.getUUID());
                    
                    if (zone.getType().getID().equals(org.sosly.villagetale.zone.type.Home.ID)) {
                        villager.getBrain().setMemory(org.sosly.villagetale.entity.MemoryModuleTypes.HOME_ZONE.get(), zoneId);
                        villager.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.HOME);
                    }
                    
                    source.sendSuccess(() ->
                        Component.literal("Assigned villager to zone: " + zone.getName()), true);
                    return 1;
                }
            }

            source.sendFailure(Component.literal("Zone not found"));
            return 0;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to assign villager: " + e.getMessage()));
            return 0;
        }
    }

    private static int unassignVillager(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            UUID villageId = VillageUUIDArgument.getVillageUUID(context, "villageUUID");
            Entity entity = EntityArgument.getEntity(context, "villager");

            if (!(entity instanceof Villager villager)) {
                source.sendFailure(Component.literal("Entity is not a villager"));
                return 0;
            }

            IVillageCapability capability = getVillageCapability(source, villageId);
            if (capability == null) return 0;

            UUID villagerUuid = villager.getUUID();
            boolean removed = false;

            for (IVillageZone zone : capability.getZones()) {
                if (zone.removeAssignedVillager(villagerUuid)) {
                    if (zone.getType().getID().equals(org.sosly.villagetale.zone.type.Home.ID)) {
                        villager.getBrain().eraseMemory(org.sosly.villagetale.entity.MemoryModuleTypes.HOME_ZONE.get());
                        villager.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.HOME);
                    }
                    
                    source.sendSuccess(() ->
                        Component.literal("Unassigned villager from zone: " + zone.getName()), true);
                    removed = true;
                }
            }

            if (!removed) {
                source.sendFailure(Component.literal("Villager was not assigned to any zones"));
                return 0;
            }

            return 1;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to unassign villager: " + e.getMessage()));
            return 0;
        }
    }
}
