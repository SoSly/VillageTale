package org.sosly.villageworks.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import org.sosly.villageworks.api.data.IVillageZone;
import org.sosly.villageworks.api.data.ZoneType;
import org.sosly.villageworks.capability.Capabilities;
import org.sosly.villageworks.command.arguments.VillageUUIDArgument;
import org.sosly.villageworks.command.arguments.ZoneTypeArgument;
import org.sosly.villageworks.command.arguments.ZoneUUIDArgument;
import org.sosly.villageworks.data.VillageData;
import org.sosly.villageworks.data.zones.PathVillageZone;
import org.sosly.villageworks.data.zones.ZoneFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ZoneCommand {

    public static void register(LiteralArgumentBuilder<CommandSourceStack> parentCommand) {
        parentCommand.then(Commands.literal("zone")
            .then(Commands.argument("villageUUID", VillageUUIDArgument.villageUUID())
                .suggests((context, builder) -> VillageUUIDArgument.suggest(context, builder))
                .then(Commands.literal("create")
                    .then(Commands.literal("aabb")
                        .then(Commands.argument("pos1", BlockPosArgument.blockPos())
                            .then(Commands.argument("pos2", BlockPosArgument.blockPos())
                                .then(Commands.argument("type", ZoneTypeArgument.zoneType())
                                    .suggests((context, builder) -> ZoneTypeArgument.suggest(context, builder))
                                    .executes(ZoneCommand::createAABBZone)
                                    .then(Commands.argument("name", StringArgumentType.string())
                                        .executes(ZoneCommand::createAABBZoneWithName))))))
                    .then(Commands.literal("radius")
                        .then(Commands.argument("center", BlockPosArgument.blockPos())
                            .then(Commands.argument("radius", IntegerArgumentType.integer(1))
                                .then(Commands.argument("type", ZoneTypeArgument.zoneType())
                                    .suggests((context, builder) -> ZoneTypeArgument.suggest(context, builder))
                                    .executes(ZoneCommand::createRadiusZone)
                                    .then(Commands.argument("name", StringArgumentType.string())
                                        .executes(ZoneCommand::createRadiusZoneWithName))))))
                    .then(Commands.literal("blockpos")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                            .then(Commands.argument("type", ZoneTypeArgument.zoneType())
                                .suggests((context, builder) -> ZoneTypeArgument.suggest(context, builder))
                                .executes(ZoneCommand::createBlockPosZone)
                                .then(Commands.argument("name", StringArgumentType.string())
                                    .executes(ZoneCommand::createBlockPosZoneWithName)))))
                    .then(Commands.literal("path")
                        .then(Commands.argument("type", ZoneTypeArgument.zoneType())
                            .suggests((context, builder) -> ZoneTypeArgument.suggest(context, builder))
                            .executes(ZoneCommand::createPathZone)
                            .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ZoneCommand::createPathZoneWithName)))))
                .then(Commands.literal("delete")
                    .then(Commands.argument("zoneUUID", ZoneUUIDArgument.zoneUUID())
                        .suggests((context, builder) -> ZoneUUIDArgument.suggest(context, builder))
                        .executes(ZoneCommand::deleteZone)))
                .then(Commands.literal("type")
                    .then(Commands.argument("zoneUUID", ZoneUUIDArgument.zoneUUID())
                        .suggests((context, builder) -> ZoneUUIDArgument.suggest(context, builder))
                        .then(Commands.argument("zonetype", ZoneTypeArgument.zoneType())
                            .suggests((context, builder) -> ZoneTypeArgument.suggest(context, builder))
                            .executes(ZoneCommand::changeZoneType))))
                .then(Commands.literal("rename")
                    .then(Commands.argument("zoneUUID", ZoneUUIDArgument.zoneUUID())
                        .suggests((context, builder) -> ZoneUUIDArgument.suggest(context, builder))
                        .then(Commands.argument("name", StringArgumentType.string())
                            .executes(ZoneCommand::renameZone))))
                .then(Commands.literal("info")
                    .then(Commands.argument("zoneUUID", ZoneUUIDArgument.zoneUUID())
                        .suggests((context, builder) -> ZoneUUIDArgument.suggest(context, builder))
                        .executes(ZoneCommand::showZoneInfo)))
                .then(Commands.literal("list")
                    .executes(ZoneCommand::listZones))
                .then(Commands.literal("path")
                    .then(Commands.argument("zoneUUID", ZoneUUIDArgument.zoneUUID())
                        .suggests((context, builder) -> ZoneUUIDArgument.suggest(context, builder))
                        .then(Commands.literal("add")
                            .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(ZoneCommand::addPathPoint)))
                        .then(Commands.literal("clear")
                            .executes(ZoneCommand::clearPath))))));
    }

    private static int createAABBZone(CommandContext<CommandSourceStack> context) {
        return createAABBZoneInternal(context, null);
    }

    private static int createAABBZoneWithName(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        return createAABBZoneInternal(context, name);
    }

    private static int createAABBZoneInternal(CommandContext<CommandSourceStack> context, String name) {
        try {
            CommandSourceStack source = context.getSource();
            UUID villageId = VillageUUIDArgument.getVillageUUID(context, "villageUUID");
            ZoneType zoneType = ZoneTypeArgument.getZoneType(context, "type");

            BlockPos pos1 = BlockPosArgument.getBlockPos(context, "pos1");
            BlockPos pos2 = BlockPosArgument.getBlockPos(context, "pos2");

            AABB bounds = new AABB(
                Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()),
                Math.max(pos1.getX(), pos2.getX()), Math.max(pos1.getY(), pos2.getY()), Math.max(pos1.getZ(), pos2.getZ())
            );

            return createZoneInVillage(source, villageId, level -> {
                int nextId = getNextZoneId(level, villageId);
                return ZoneFactory.createAABBZone(zoneType, nextId, name, bounds, level);
            });

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to create AABB zone: " + e.getMessage()));
            return 0;
        }
    }

    private static int createRadiusZone(CommandContext<CommandSourceStack> context) {
        return createRadiusZoneInternal(context, null);
    }

    private static int createRadiusZoneWithName(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        return createRadiusZoneInternal(context, name);
    }

    private static int createRadiusZoneInternal(CommandContext<CommandSourceStack> context, String name) {
        try {
            CommandSourceStack source = context.getSource();
            UUID villageId = VillageUUIDArgument.getVillageUUID(context, "villageUUID");
            ZoneType zoneType = ZoneTypeArgument.getZoneType(context, "type");

            BlockPos center = BlockPosArgument.getBlockPos(context, "center");
            int radius = IntegerArgumentType.getInteger(context, "radius");

            return createZoneInVillage(source, villageId, level -> {
                int nextId = getNextZoneId(level, villageId);
                return ZoneFactory.createRadiusZone(zoneType, nextId, name, center, radius, level);
            });

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to create radius zone: " + e.getMessage()));
            return 0;
        }
    }

    private static int createBlockPosZone(CommandContext<CommandSourceStack> context) {
        return createBlockPosZoneInternal(context, null);
    }

    private static int createBlockPosZoneWithName(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        return createBlockPosZoneInternal(context, name);
    }

    private static int createBlockPosZoneInternal(CommandContext<CommandSourceStack> context, String name) {
        try {
            CommandSourceStack source = context.getSource();
            UUID villageId = VillageUUIDArgument.getVillageUUID(context, "villageUUID");
            ZoneType zoneType = ZoneTypeArgument.getZoneType(context, "type");

            BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");

            return createZoneInVillage(source, villageId, level -> {
                int nextId = getNextZoneId(level, villageId);
                return ZoneFactory.createBlockPosZone(zoneType, nextId, name, pos, level);
            });

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to create blockpos zone: " + e.getMessage()));
            return 0;
        }
    }

    private static int createPathZone(CommandContext<CommandSourceStack> context) {
        return createPathZoneInternal(context, null);
    }

    private static int createPathZoneWithName(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        return createPathZoneInternal(context, name);
    }

    private static int createPathZoneInternal(CommandContext<CommandSourceStack> context, String name) {
        try {
            CommandSourceStack source = context.getSource();
            UUID villageId = VillageUUIDArgument.getVillageUUID(context, "villageUUID");
            ZoneType zoneType = ZoneTypeArgument.getZoneType(context, "type");

            return createZoneInVillage(source, villageId, level -> {
                int nextId = getNextZoneId(level, villageId);
                return ZoneFactory.createPathZone(zoneType, nextId, name, new ArrayList<>(), level);
            });

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to create path zone: " + e.getMessage()));
            return 0;
        }
    }

    private static int deleteZone(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            UUID villageId = VillageUUIDArgument.getVillageUUID(context, "villageUUID");
            UUID zoneId = ZoneUUIDArgument.getZoneUUID(context, "zoneUUID");

            ServerLevel level = source.getLevel();

            return level.getCapability(Capabilities.VILLAGES_CAPABILITY)
                .map(manager -> {
                    VillageData village = manager.getVillageById(villageId);
                    if (village == null) {
                        source.sendFailure(Component.literal("Village not found"));
                        return 0;
                    }

                    ChunkPos villageChunk = village.getVillageStartingChunk();
                    LevelChunk chunk = level.getChunk(villageChunk.x, villageChunk.z);

                    return chunk.getCapability(Capabilities.VILLAGE_CAPABILITY)
                        .map(villageCapability -> {
                            boolean removed = villageCapability.removeZone(zoneId);
                            if (removed) {
                                source.sendSuccess(() ->
                                    Component.literal("Deleted zone " + zoneId), true);
                                return 1;
                            } else {
                                source.sendFailure(Component.literal("Zone not found in village"));
                                return 0;
                            }
                        })
                        .orElse(0);
                })
                .orElse(0);

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to delete zone: " + e.getMessage()));
            return 0;
        }
    }

    private static int changeZoneType(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            UUID villageId = VillageUUIDArgument.getVillageUUID(context, "villageUUID");
            UUID zoneId = ZoneUUIDArgument.getZoneUUID(context, "zoneUUID");
            ZoneType newZoneType = ZoneTypeArgument.getZoneType(context, "zonetype");
            
            source.sendFailure(Component.literal("Zone type changing not implemented yet"));
            return 0;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to change zone type: " + e.getMessage()));
            return 0;
        }
    }

    private static int renameZone(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            String villageUUIDStr = StringArgumentType.getString(context, "villageUUID");
            String zoneUUIDStr = StringArgumentType.getString(context, "zoneUUID");
            String newName = StringArgumentType.getString(context, "name");

            UUID villageId;
            UUID zoneId;
            try {
                villageId = UUID.fromString(villageUUIDStr);
                zoneId = UUID.fromString(zoneUUIDStr);
            } catch (IllegalArgumentException e) {
                source.sendFailure(Component.literal("Invalid UUID format"));
                return 0;
            }

            ServerLevel level = source.getLevel();

            return level.getCapability(Capabilities.VILLAGES_CAPABILITY)
                .map(manager -> {
                    VillageData village = manager.getVillageById(villageId);
                    if (village == null) {
                        source.sendFailure(Component.literal("Village not found"));
                        return 0;
                    }

                    ChunkPos villageChunk = village.getVillageStartingChunk();
                    LevelChunk chunk = level.getChunk(villageChunk.x, villageChunk.z);

                    return chunk.getCapability(Capabilities.VILLAGE_CAPABILITY)
                        .map(villageCapability -> {
                            List<IVillageZone> zones = villageCapability.getZones();
                            for (IVillageZone zone : zones) {
                                if (zone.getUUID().equals(zoneId)) {
                                    zone.setName(newName);
                                    source.sendSuccess(() ->
                                        Component.literal("Renamed zone to '" + newName + "'"), true);
                                    return 1;
                                }
                            }
                            source.sendFailure(Component.literal("Zone not found in village"));
                            return 0;
                        })
                        .orElse(0);
                })
                .orElse(0);

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to rename zone: " + e.getMessage()));
            return 0;
        }
    }

    private static int showZoneInfo(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            UUID villageId = VillageUUIDArgument.getVillageUUID(context, "villageUUID");
            UUID zoneId = ZoneUUIDArgument.getZoneUUID(context, "zoneUUID");

            ServerLevel level = source.getLevel();

            return level.getCapability(Capabilities.VILLAGES_CAPABILITY)
                .map(manager -> {
                    VillageData village = manager.getVillageById(villageId);
                    if (village == null) {
                        source.sendFailure(Component.literal("Village not found"));
                        return 0;
                    }

                    ChunkPos villageChunk = village.getVillageStartingChunk();
                    LevelChunk chunk = level.getChunk(villageChunk.x, villageChunk.z);

                    return chunk.getCapability(Capabilities.VILLAGE_CAPABILITY)
                        .map(villageCapability -> {
                            List<IVillageZone> zones = villageCapability.getZones();
                            for (IVillageZone zone : zones) {
                                if (zone.getUUID().equals(zoneId)) {
                                    source.sendSuccess(() ->
                                        Component.literal("Zone: " + zone.getName()), false);
                                    source.sendSuccess(() ->
                                        Component.literal("UUID: " + zone.getUUID()), false);
                                    source.sendSuccess(() ->
                                        Component.literal("Type: " + zone.getType()), false);
                                    source.sendSuccess(() ->
                                        Component.literal("Shape: " + zone.getShape()), false);
                                    
                                    showZoneBounds(source, zone);
                                    showZonePOIs(source, zone, level);
                                    
                                    return 1;
                                }
                            }
                            source.sendFailure(Component.literal("Zone not found in village"));
                            return 0;
                        })
                        .orElse(0);
                })
                .orElse(0);

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to show zone info: " + e.getMessage()));
            return 0;
        }
    }

    private static int listZones(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            UUID villageId = VillageUUIDArgument.getVillageUUID(context, "villageUUID");

            ServerLevel level = source.getLevel();

            return level.getCapability(Capabilities.VILLAGES_CAPABILITY)
                .map(manager -> {
                    VillageData village = manager.getVillageById(villageId);
                    if (village == null) {
                        source.sendFailure(Component.literal("Village not found"));
                        return 0;
                    }

                    ChunkPos villageChunk = village.getVillageStartingChunk();
                    LevelChunk chunk = level.getChunk(villageChunk.x, villageChunk.z);

                    return chunk.getCapability(Capabilities.VILLAGE_CAPABILITY)
                        .map(villageCapability -> {
                            List<IVillageZone> zones = villageCapability.getZones();

                            if (zones.isEmpty()) {
                                source.sendSuccess(() ->
                                    Component.literal("No zones found in village"), false);
                                return 1;
                            }

                            source.sendSuccess(() ->
                                Component.literal("Zones in " + village.getVillageName() + ":"), false);

                            for (IVillageZone zone : zones) {
                                source.sendSuccess(() ->
                                    Component.literal("- " + zone.getName() + " (" + zone.getType() + ", " + zone.getShape() + ") UUID: " + zone.getUUID()), false);
                            }

                            return zones.size();
                        })
                        .orElse(0);
                })
                .orElse(0);

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to list zones: " + e.getMessage()));
            return 0;
        }
    }

    private static int addPathPoint(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            String villageUUIDStr = StringArgumentType.getString(context, "villageUUID");
            String zoneUUIDStr = StringArgumentType.getString(context, "zoneUUID");
            BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");

            UUID villageId;
            UUID zoneId;
            try {
                villageId = UUID.fromString(villageUUIDStr);
                zoneId = UUID.fromString(zoneUUIDStr);
            } catch (IllegalArgumentException e) {
                source.sendFailure(Component.literal("Invalid UUID format"));
                return 0;
            }

            ServerLevel level = source.getLevel();

            return level.getCapability(Capabilities.VILLAGES_CAPABILITY)
                .map(manager -> {
                    VillageData village = manager.getVillageById(villageId);
                    if (village == null) {
                        source.sendFailure(Component.literal("Village not found"));
                        return 0;
                    }

                    ChunkPos villageChunk = village.getVillageStartingChunk();
                    LevelChunk chunk = level.getChunk(villageChunk.x, villageChunk.z);

                    return chunk.getCapability(Capabilities.VILLAGE_CAPABILITY)
                        .map(villageCapability -> {
                            List<IVillageZone> zones = villageCapability.getZones();
                            for (IVillageZone zone : zones) {
                                if (zone.getUUID().equals(zoneId) && zone instanceof PathVillageZone pathZone) {
                                    pathZone.addPoint(pos);
                                    source.sendSuccess(() ->
                                        Component.literal("Added point " + pos.toShortString() + " to path zone"), true);
                                    return 1;
                                }
                            }
                            source.sendFailure(Component.literal("Path zone not found in village"));
                            return 0;
                        })
                        .orElse(0);
                })
                .orElse(0);

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to add path point: " + e.getMessage()));
            return 0;
        }
    }

    private static int clearPath(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            UUID villageId = VillageUUIDArgument.getVillageUUID(context, "villageUUID");
            UUID zoneId = ZoneUUIDArgument.getZoneUUID(context, "zoneUUID");

            ServerLevel level = source.getLevel();

            return level.getCapability(Capabilities.VILLAGES_CAPABILITY)
                .map(manager -> {
                    VillageData village = manager.getVillageById(villageId);
                    if (village == null) {
                        source.sendFailure(Component.literal("Village not found"));
                        return 0;
                    }

                    ChunkPos villageChunk = village.getVillageStartingChunk();
                    LevelChunk chunk = level.getChunk(villageChunk.x, villageChunk.z);

                    return chunk.getCapability(Capabilities.VILLAGE_CAPABILITY)
                        .map(villageCapability -> {
                            List<IVillageZone> zones = villageCapability.getZones();
                            for (IVillageZone zone : zones) {
                                if (zone.getUUID().equals(zoneId) && zone instanceof PathVillageZone pathZone) {
                                    pathZone.clearPath();
                                    source.sendSuccess(() ->
                                        Component.literal("Cleared path zone"), true);
                                    return 1;
                                }
                            }
                            source.sendFailure(Component.literal("Path zone not found in village"));
                            return 0;
                        })
                        .orElse(0);
                })
                .orElse(0);

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to clear path: " + e.getMessage()));
            return 0;
        }
    }

    private static int createZoneInVillage(CommandSourceStack source, UUID villageId, ZoneCreator factory) {
        ServerLevel level = source.getLevel();

        return level.getCapability(Capabilities.VILLAGES_CAPABILITY)
            .map(manager -> {
                VillageData village = manager.getVillageById(villageId);
                if (village == null) {
                    source.sendFailure(Component.literal("Village not found"));
                    return 0;
                }

                ChunkPos villageChunk = village.getVillageStartingChunk();
                LevelChunk chunk = level.getChunk(villageChunk.x, villageChunk.z);

                var capability = chunk.getCapability(Capabilities.VILLAGE_CAPABILITY).orElse(null);
                if (capability == null) {
                    source.sendFailure(Component.literal("Village capability not found at chunk " + villageChunk + 
                        ". The chunk may need to be reloaded. Try leaving and re-entering the area."));
                    return 0;
                }
                
                try {
                    IVillageZone zone = factory.createZone(level);
                    capability.addZone(zone);

                    source.sendSuccess(() ->
                        Component.literal("Created zone '" + zone.getName() + "' with UUID: " + zone.getUUID()), true);
                    return 1;

                } catch (Exception e) {
                    source.sendFailure(Component.literal("Failed to create zone: " + e.getMessage()));
                    return 0;
                }
            })
            .orElse(0);
    }

    private static int getNextZoneId(ServerLevel level, UUID villageId) {
        return level.getCapability(Capabilities.VILLAGES_CAPABILITY)
            .map(manager -> {
                VillageData village = manager.getVillageById(villageId);
                if (village == null) {
                    return 1;
                }

                ChunkPos villageChunk = village.getVillageStartingChunk();
                LevelChunk chunk = level.getChunk(villageChunk.x, villageChunk.z);

                return chunk.getCapability(Capabilities.VILLAGE_CAPABILITY)
                    .map(villageCapability -> {
                        List<IVillageZone> zones = villageCapability.getZones();
                        return zones.size() + 1;
                    })
                    .orElse(1);
            })
            .orElse(1);
    }

    private static void showZoneBounds(CommandSourceStack source, IVillageZone zone) {
        if (zone instanceof org.sosly.villageworks.data.zones.AABBVillageZone aabbZone) {
            var bounds = aabbZone.getAABB();
            source.sendSuccess(() ->
                Component.literal("Bounds: (" + (int)bounds.minX + ", " + (int)bounds.minY + ", " + (int)bounds.minZ + 
                               ") to (" + (int)bounds.maxX + ", " + (int)bounds.maxY + ", " + (int)bounds.maxZ + ")"), false);
        } else if (zone instanceof org.sosly.villageworks.data.zones.RadiusVillageZone radiusZone) {
            var center = radiusZone.getCenter();
            source.sendSuccess(() ->
                Component.literal("Center: " + center.toShortString() + ", Radius: " + radiusZone.getRadius()), false);
        } else if (zone instanceof org.sosly.villageworks.data.zones.BlockPosVillageZone blockPosZone) {
            var pos = blockPosZone.getBlockPos();
            source.sendSuccess(() ->
                Component.literal("Position: " + pos.toShortString()), false);
        } else if (zone instanceof org.sosly.villageworks.data.zones.PathVillageZone pathZone) {
            var path = pathZone.getPath();
            if (path.isEmpty()) {
                source.sendSuccess(() ->
                    Component.literal("Path: Empty"), false);
            } else {
                source.sendSuccess(() ->
                    Component.literal("Path: " + path.size() + " points"), false);
                for (int i = 0; i < Math.min(path.size(), 5); i++) {
                    final int index = i;
                    source.sendSuccess(() ->
                        Component.literal("  " + index + ": " + path.get(index).toShortString()), false);
                }
                if (path.size() > 5) {
                    source.sendSuccess(() ->
                        Component.literal("  ... and " + (path.size() - 5) + " more points"), false);
                }
            }
        }
    }
    
    private static void showZonePOIs(CommandSourceStack source, IVillageZone zone, ServerLevel level) {
        var pois = zone.getPOIs();
        if (pois.isEmpty()) {
            source.sendSuccess(() ->
                Component.literal("POIs: None (zone type: " + zone.getType() + ")"), false);
        } else {
            var poiList = pois.get();
            source.sendSuccess(() ->
                Component.literal("POIs: " + poiList.size() + " found"), false);
            for (int i = 0; i < Math.min(poiList.size(), 10); i++) {
                final int index = i;
                final BlockPos pos = poiList.get(index);
                String blockName = getBlockDisplayName(level, pos);
                source.sendSuccess(() ->
                    Component.literal("  " + index + ": " + blockName + " at " + pos.toShortString()), false);
            }
            if (poiList.size() > 10) {
                source.sendSuccess(() ->
                    Component.literal("  ... and " + (poiList.size() - 10) + " more POIs"), false);
            }
        }
    }
    
    private static String getBlockDisplayName(ServerLevel level, BlockPos pos) {
        var blockState = level.getBlockState(pos);
        var block = blockState.getBlock();
        String blockName = block.getDescriptionId();
        
        // Convert from translation key to display name
        if (blockName.startsWith("block.")) {
            // Remove "block." prefix and convert underscores to spaces
            String cleanName = blockName.substring(6);
            
            // Handle mod blocks vs vanilla blocks
            if (cleanName.contains(".")) {
                String[] parts = cleanName.split("\\.", 2);
                String modId = parts[0];
                String blockId = parts[1];
                
                if ("villageworks".equals(modId)) {
                    // Special handling for our blocks
                    return switch (blockId) {
                        case "townhall" -> "Town Hall Block";
                        default -> formatBlockName(blockId);
                    };
                } else {
                    // Other mod blocks
                    return formatBlockName(blockId) + " (" + modId + ")";
                }
            } else {
                // Vanilla blocks
                return switch (cleanName) {
                    case "chest" -> "Chest";
                    case "barrel" -> "Barrel";
                    case "shulker_box" -> "Shulker Box";
                    case "white_bed", "orange_bed", "magenta_bed", "light_blue_bed", 
                         "yellow_bed", "lime_bed", "pink_bed", "gray_bed", "light_gray_bed", 
                         "cyan_bed", "purple_bed", "blue_bed", "brown_bed", "green_bed", 
                         "red_bed", "black_bed" -> {
                        String color = cleanName.replace("_bed", "");
                        yield formatBlockName(color) + " Bed";
                    }
                    default -> formatBlockName(cleanName);
                };
            }
        }
        
        return blockName; // Fallback to raw translation key
    }
    
    private static String formatBlockName(String name) {
        return java.util.Arrays.stream(name.split("_"))
            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
            .reduce((a, b) -> a + " " + b)
            .orElse(name);
    }

    @FunctionalInterface
    private interface ZoneCreator {
        IVillageZone createZone(ServerLevel level);
    }
}
