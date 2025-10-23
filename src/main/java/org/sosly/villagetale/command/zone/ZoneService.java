package org.sosly.villagetale.command.zone;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.IZoneShape;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.command.Result;
import org.sosly.villagetale.data.matchers.ItemOrTagMatcher;
import org.sosly.villagetale.data.VillageInfo;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.zone.Zone;
import org.sosly.villagetale.zone.shape.Box;
import org.sosly.villagetale.zone.shape.Cylinder;
import org.sosly.villagetale.zone.shape.Point;
import org.sosly.villagetale.zone.type.AbstractZoneType;
import org.sosly.villagetale.zone.shape.Route;
import org.sosly.villagetale.zone.type.Home;
import org.sosly.villagetale.zone.type.TownHall;
import org.sosly.villagetale.helper.ZoneValidationHelper;

public class ZoneService {

    public static VillageInfo getVillageInfo(ServerLevel level, UUID villageId) {
        IVillagesCapability villages = level.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
        if (villages == null) {
            return null;
        }

        return villages.getVillageById(villageId);
    }

    public static IVillageCapability getVillageCapability(ServerLevel level, UUID villageId) {
        IVillagesCapability villages = level.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
        if (villages == null) {
            return null;
        }

        VillageInfo village = villages.getVillageById(villageId);
        if (village == null) {
            return null;
        }

        ChunkPos villageChunk = village.getVillageStartingChunk();
        LevelChunk chunk = level.getChunk(villageChunk.x, villageChunk.z);
        return chunk.getCapability(Capabilities.VILLAGE_CAPABILITY).orElse(null);
    }

    public static Result createBoxZone(ServerLevel level, UUID villageId, BlockPos pos1, BlockPos pos2,
                                       ResourceLocation zoneType, String name) {
        if (zoneType.equals(TownHall.ID)) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.townhall_auto_managed", VillageTale.MOD_ID)));
        }

        VillageInfo villageInfo = getVillageInfo(level, villageId);
        if (villageInfo == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.village_capability_not_found", VillageTale.MOD_ID)));
        }

        IVillageCapability capability = getVillageCapability(level, villageId);
        if (capability == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.village_capability_not_found", VillageTale.MOD_ID)));
        }

        AABB bounds = new AABB(
                Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()),
                Math.max(pos1.getX(), pos2.getX()) + 1, Math.max(pos1.getY(), pos2.getY()) + 1, Math.max(pos1.getZ(), pos2.getZ()) + 1
        );

        if (!ZoneValidationHelper.isBoxWithinBoundary(villageInfo, bounds)) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.outside_boundary", VillageTale.MOD_ID)));
        }

        Zone zone = Box.builder(level, capability, capability.getZones().size())
                .setBounds(bounds)
                .setType(zoneType)
                .build();

        if (name != null) {
            zone.setName(name);
        }

        capability.addZone(zone);
        return Result.success(Component.translatable(
                String.format("%s.command.zone.box_created", VillageTale.MOD_ID), zone.getName()));
    }

    public static Result createCylinderZone(ServerLevel level, UUID villageId, BlockPos center, int radius, int height,
                                            ResourceLocation zoneType, String name) {
        if (zoneType.equals(TownHall.ID)) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.townhall_auto_managed", VillageTale.MOD_ID)));
        }

        VillageInfo villageInfo = getVillageInfo(level, villageId);
        if (villageInfo == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.village_capability_not_found", VillageTale.MOD_ID)));
        }

        if (!ZoneValidationHelper.isCylinderWithinBoundary(villageInfo, center, radius)) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.outside_boundary", VillageTale.MOD_ID)));
        }

        IVillageCapability capability = getVillageCapability(level, villageId);
        if (capability == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.village_capability_not_found", VillageTale.MOD_ID)));
        }

        Zone zone = Cylinder.builder(level, capability, capability.getZones().size())
                .setBaseCenter(center)
                .setRadius(radius)
                .setHeight(height)
                .setType(zoneType)
                .build();

        if (name != null) {
            zone.setName(name);
        }

        capability.addZone(zone);
        return Result.success(Component.translatable(
                String.format("%s.command.zone.cylinder_created", VillageTale.MOD_ID), zone.getName()));
    }

    public static Result createPointZone(ServerLevel level, UUID villageId, BlockPos pos,
                                        ResourceLocation zoneType, String name) {
        if (zoneType.equals(TownHall.ID)) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.townhall_auto_managed", VillageTale.MOD_ID)));
        }

        VillageInfo villageInfo = getVillageInfo(level, villageId);
        if (villageInfo == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.village_capability_not_found", VillageTale.MOD_ID)));
        }

        if (!ZoneValidationHelper.isPositionWithinBoundary(villageInfo, pos)) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.outside_boundary", VillageTale.MOD_ID)));
        }

        IVillageCapability capability = getVillageCapability(level, villageId);
        if (capability == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.village_capability_not_found", VillageTale.MOD_ID)));
        }

        Zone zone = Point.builder(level, capability, capability.getZones().size())
                .setPos(pos)
                .setType(zoneType)
                .build();

        if (name != null) {
            zone.setName(name);
        }

        capability.addZone(zone);
        return Result.success(Component.translatable(
                String.format("%s.command.zone.point_created", VillageTale.MOD_ID), zone.getName()));
    }

    public static Result createRouteZone(ServerLevel level, UUID villageId,
                                        ResourceLocation zoneType, String name) {
        if (zoneType.equals(TownHall.ID)) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.townhall_auto_managed", VillageTale.MOD_ID)));
        }

        IVillageCapability capability = getVillageCapability(level, villageId);
        if (capability == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.village_capability_not_found", VillageTale.MOD_ID)));
        }

        Zone zone = Route.builder(level, capability, capability.getZones().size())
                .setType(zoneType)
                .build();

        if (name != null) {
            zone.setName(name);
        }

        capability.addZone(zone);
        return Result.success(Component.translatable(
                String.format("%s.command.zone.route_created", VillageTale.MOD_ID), zone.getName()));
    }

    public static Result deleteZone(ServerLevel level, UUID villageId, UUID zoneId) {
        IVillageCapability capability = getVillageCapability(level, villageId);
        if (capability == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.village_capability_not_found", VillageTale.MOD_ID)));
        }

        IVillageZone toRemove = capability.getZones().stream()
                .filter(zone -> zone.getUUID().equals(zoneId))
                .findFirst()
                .orElse(null);

        if (toRemove == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.not_found", VillageTale.MOD_ID), zoneId));
        }

        if (toRemove.getType().getID().equals(TownHall.ID)) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.cannot_delete_townhall", VillageTale.MOD_ID)));
        }

        String zoneName = toRemove.getName();
        boolean removed = capability.removeZone(zoneId);

        if (removed) {
            return Result.success(Component.translatable(
                    String.format("%s.command.zone.deleted", VillageTale.MOD_ID), zoneName));
        } else {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.deletion_failed", VillageTale.MOD_ID), zoneName));
        }
    }

    public static Result listZones(ServerLevel level, UUID villageId) {
        IVillageCapability capability = getVillageCapability(level, villageId);
        if (capability == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.village_capability_not_found", VillageTale.MOD_ID)));
        }

        List<IVillageZone> zones = capability.getZones();
        if (zones.isEmpty()) {
            return Result.success(Component.translatable(
                    String.format("%s.command.zone.no_zones", VillageTale.MOD_ID)));
        }

        StringBuilder message = new StringBuilder();
        message.append(Component.translatable(
                String.format("%s.command.zone.list_header", VillageTale.MOD_ID)).getString()).append("\n");

        for (IVillageZone zone : zones) {
            message.append(String.format("- %s (Type: %s, ID: %s)\n",
                    zone.getName(), zone.getType().getID(), zone.getUUID()));
        }

        return Result.success(Component.literal(message.toString().trim()));
    }

    public static void displayZoneInfo(IVillageZone zone, java.util.function.Consumer<Component> sender) {
        sender.accept(Component.translatable(String.format("%s.command.zone.info_header", VillageTale.MOD_ID)));
        sender.accept(Component.translatable(String.format("%s.command.zone.info_name", VillageTale.MOD_ID), zone.getName()));
        sender.accept(Component.translatable(String.format("%s.command.zone.info_uuid", VillageTale.MOD_ID), zone.getUUID()));
        sender.accept(Component.translatable(String.format("%s.command.zone.info_type", VillageTale.MOD_ID), zone.getType().getID()));

        if (zone instanceof Zone zImpl) {
            IZoneShape shape = zImpl.getShape();
            if (shape instanceof Box rect) {
                AABB bounds = rect.getBounds();
                sender.accept(Component.translatable(String.format("%s.command.zone.info_shape_box", VillageTale.MOD_ID),
                        (int) bounds.minX, (int) bounds.minY, (int) bounds.minZ,
                        (int) (bounds.maxX - 1), (int) (bounds.maxY - 1), (int) (bounds.maxZ - 1)));
            } else if (shape instanceof Cylinder cylinder) {
                sender.accept(Component.translatable(String.format("%s.command.zone.info_shape_cylinder", VillageTale.MOD_ID),
                        cylinder.getStartPosition().toShortString(), cylinder.getRadius(), cylinder.getHeight()));
            } else if (shape instanceof Point point) {
                sender.accept(Component.translatable(String.format("%s.command.zone.info_shape_point", VillageTale.MOD_ID),
                        point.getPos().toShortString()));
            } else if (shape instanceof Route route) {
                List<BlockPos> path = route.getPath();
                sender.accept(Component.translatable(String.format("%s.command.zone.info_shape_route", VillageTale.MOD_ID),
                        path.size()));
            }
        }

        List<UUID> assigned = zone.getAssignedVillagers();
        sender.accept(Component.translatable(String.format("%s.command.zone.info_assigned", VillageTale.MOD_ID), assigned.size()));

        List<ItemStack> filters = zone.getFilter();
        if (!filters.isEmpty()) {
            sender.accept(Component.translatable(String.format("%s.command.zone.info_filter_header", VillageTale.MOD_ID)));
            for (ItemStack filter : filters) {
                sender.accept(Component.literal("  - " + filter.getHoverName().getString() + " x" + filter.getCount()));
            }
        }

        Set<ResourceLocation> entityTypeFilter = zone.getEntityTypeFilter();
        if (!entityTypeFilter.isEmpty()) {
            sender.accept(Component.translatable(String.format("%s.command.zone.info_entity_type_filter_header", VillageTale.MOD_ID)));
            for (ResourceLocation entityTypeId : entityTypeFilter) {
                sender.accept(Component.literal("  - " + entityTypeId.toString()));
            }
        }
    }

    public static Result addRoutePoint(ServerLevel level, UUID villageId, UUID zoneId, BlockPos pos) {
        VillageInfo villageInfo = getVillageInfo(level, villageId);
        if (villageInfo == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.village_capability_not_found", VillageTale.MOD_ID)));
        }

        if (!ZoneValidationHelper.isPositionWithinBoundary(villageInfo, pos)) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.outside_boundary", VillageTale.MOD_ID)));
        }

        IVillageCapability capability = getVillageCapability(level, villageId);
        if (capability == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.village_capability_not_found", VillageTale.MOD_ID)));
        }

        for (IVillageZone zone : capability.getZones()) {
            if (zone.getUUID().equals(zoneId)) {
                if (zone instanceof Zone zImpl && zImpl.getShape() instanceof Route route) {
                    route.addPoint(pos);
                    return Result.success(Component.translatable(
                            String.format("%s.command.zone.route_point_added", VillageTale.MOD_ID), pos.toShortString()));
                } else {
                    return Result.failure(Component.translatable(
                            String.format("%s.command.zone.not_route_type", VillageTale.MOD_ID)));
                }
            }
        }

        return Result.failure(Component.translatable(
                String.format("%s.command.zone.not_found", VillageTale.MOD_ID), zoneId));
    }

    public static Result clearRoute(ServerLevel level, UUID villageId, UUID zoneId) {
        IVillageCapability capability = getVillageCapability(level, villageId);
        if (capability == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.village_capability_not_found", VillageTale.MOD_ID)));
        }

        for (IVillageZone zone : capability.getZones()) {
            if (zone.getUUID().equals(zoneId)) {
                if (zone instanceof Zone zImpl && zImpl.getShape() instanceof Route route) {
                    route.clearPath();
                    return Result.success(Component.translatable(
                            String.format("%s.command.zone.route_cleared", VillageTale.MOD_ID)));
                } else {
                    return Result.failure(Component.translatable(
                            String.format("%s.command.zone.not_route_type", VillageTale.MOD_ID)));
                }
            }
        }

        return Result.failure(Component.translatable(
                String.format("%s.command.zone.not_found", VillageTale.MOD_ID), zoneId));
    }

    public static Result assignVillager(ServerLevel level, UUID villageId, UUID zoneId, Villager villager) {
        IVillageCapability capability = getVillageCapability(level, villageId);
        if (capability == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.village_capability_not_found", VillageTale.MOD_ID)));
        }

        for (IVillageZone zone : capability.getZones()) {
            if (zone.getUUID().equals(zoneId)) {
                zone.addAssignedVillager(villager.getUUID());

                if (zone.getType().getID().equals(Home.ID)) {
                    // Clear any existing home-related memories before setting new one
                    villager.getBrain().eraseMemory(MemoryModuleType.HOME);
                    villager.getBrain().eraseMemory(MemoryModuleType.LAST_SLEPT);
                    villager.getBrain().setMemory(MemoryModuleTypes.HOME_ZONE.get(), zoneId);
                } else if (villager.getProfession().isValidWorkZone(zone)) {
                    // Clear any existing work-related memories before setting new one
                    villager.getBrain().eraseMemory(MemoryModuleTypes.WORK_POS.get());
                    villager.getBrain().eraseMemory(MemoryModuleTypes.NEAREST_WORKSTATION.get());
                    villager.getBrain().eraseMemory(MemoryModuleTypes.NEAREST_EMPTY_FARMLAND.get());
                    villager.getBrain().eraseMemory(MemoryModuleTypes.NEAREST_HARVESTABLE_CROP.get());
                    villager.getBrain().eraseMemory(MemoryModuleTypes.NEAREST_TILLABLE_SOIL.get());
                    villager.getBrain().eraseMemory(MemoryModuleTypes.CURRENT_RECIPE.get());
                    villager.getBrain().setMemory(MemoryModuleTypes.WORK_ZONE.get(), zoneId);
                }

                return Result.success(Component.translatable(
                        String.format("%s.command.zone.villager_assigned", VillageTale.MOD_ID), zone.getName()));
            }
        }

        return Result.failure(Component.translatable(
                String.format("%s.command.zone.not_found", VillageTale.MOD_ID), zoneId));
    }

    public static Result unassignVillager(ServerLevel level, UUID villageId, Villager villager) {
        IVillageCapability capability = getVillageCapability(level, villageId);
        if (capability == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.village_capability_not_found", VillageTale.MOD_ID)));
        }

        UUID villagerUuid = villager.getUUID();
        boolean removed = false;
        String zoneName = null;

        for (IVillageZone zone : capability.getZones()) {
            if (zone.removeAssignedVillager(villagerUuid)) {
                if (zone.getType().getID().equals(Home.ID)) {
                    // Clear all home-related memories
                    villager.getBrain().eraseMemory(MemoryModuleTypes.HOME_ZONE.get());
                    villager.getBrain().eraseMemory(MemoryModuleType.HOME);
                    villager.getBrain().eraseMemory(MemoryModuleType.LAST_SLEPT);
                } else if (villager.getProfession().isValidWorkZone(zone)) {
                    // Clear all work-related memories
                    villager.getBrain().eraseMemory(MemoryModuleTypes.WORK_ZONE.get());
                    villager.getBrain().eraseMemory(MemoryModuleTypes.WORK_POS.get());
                    villager.getBrain().eraseMemory(MemoryModuleTypes.NEAREST_WORKSTATION.get());
                    villager.getBrain().eraseMemory(MemoryModuleTypes.NEAREST_EMPTY_FARMLAND.get());
                    villager.getBrain().eraseMemory(MemoryModuleTypes.NEAREST_HARVESTABLE_CROP.get());
                    villager.getBrain().eraseMemory(MemoryModuleTypes.NEAREST_TILLABLE_SOIL.get());
                    villager.getBrain().eraseMemory(MemoryModuleTypes.CURRENT_RECIPE.get());
                }
                zoneName = zone.getName();
                removed = true;
            }
        }

        if (!removed) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.villager_not_assigned", VillageTale.MOD_ID)));
        }

        return Result.success(Component.translatable(
                String.format("%s.command.zone.villager_unassigned", VillageTale.MOD_ID), zoneName));
    }

    public static Result addFilterItem(ServerLevel level, UUID villageId, UUID zoneId, ResourceLocation itemId) {
        IVillageCapability capability = getVillageCapability(level, villageId);
        if (capability == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.village_capability_not_found", VillageTale.MOD_ID)));
        }

        IVillageZone zone = capability.getZones().stream()
                .filter(z -> z.getUUID().equals(zoneId))
                .findFirst()
                .orElse(null);

        if (zone == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.not_found", VillageTale.MOD_ID), zoneId));
        }

        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == Items.AIR) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.invalid_item", VillageTale.MOD_ID), itemId));
        }

        if (!(zone.getType() instanceof AbstractZoneType abstractZoneType)) {
            ItemStack newStack = new ItemStack(item);
            List<ItemStack> currentItems = zone.getFilter();

            for (ItemStack existing : currentItems) {
                if (ItemStack.isSameItem(existing, newStack)) {
                    return Result.failure(Component.translatable(
                            String.format("%s.command.zone.filter_item_already_exists", VillageTale.MOD_ID), itemId));
                }
            }

            List<ItemStack> updatedItems = new ArrayList<>(currentItems);
            updatedItems.add(newStack);
            zone.setFilter(updatedItems);

            return Result.success(Component.translatable(
                    String.format("%s.command.zone.filter_item_added", VillageTale.MOD_ID), itemId, zone.getName()));
        }

        ItemOrTagMatcher validFilter = abstractZoneType.getItemFilter();
        if (!validFilter.isEmpty() && !validFilter.matches(item)) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.invalid_filter_for_zone_type", VillageTale.MOD_ID),
                    itemId, zone.getType().getID()));
        }

        ItemStack newStack = new ItemStack(item);
        List<ItemStack> currentItems = zone.getFilter();

        for (ItemStack existing : currentItems) {
            if (ItemStack.isSameItem(existing, newStack)) {
                return Result.failure(Component.translatable(
                        String.format("%s.command.zone.filter_item_already_exists", VillageTale.MOD_ID), itemId));
            }
        }

        List<ItemStack> updatedItems = new ArrayList<>(currentItems);
        updatedItems.add(newStack);
        zone.setFilter(updatedItems);

        return Result.success(Component.translatable(
                String.format("%s.command.zone.filter_item_added", VillageTale.MOD_ID), itemId, zone.getName()));
    }

    public static Result removeFilterItem(ServerLevel level, UUID villageId, UUID zoneId, ResourceLocation itemId) {
        IVillageCapability capability = getVillageCapability(level, villageId);
        if (capability == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.village_capability_not_found", VillageTale.MOD_ID)));
        }

        IVillageZone zone = capability.getZones().stream()
                .filter(z -> z.getUUID().equals(zoneId))
                .findFirst()
                .orElse(null);

        if (zone == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.not_found", VillageTale.MOD_ID), zoneId));
        }

        // Create ItemStack to match against
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == Items.AIR) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.invalid_item", VillageTale.MOD_ID), itemId));
        }

        ItemStack targetStack = new ItemStack(item);
        List<ItemStack> currentItems = zone.getFilter();
        List<ItemStack> updatedItems = new ArrayList<>();
        boolean found = false;

        for (ItemStack existing : currentItems) {
            if (ItemStack.isSameItem(existing, targetStack)) {
                found = true;
            } else {
                updatedItems.add(existing);
            }
        }

        if (!found) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.filter_item_not_found", VillageTale.MOD_ID), itemId));
        }

        zone.setFilter(updatedItems);

        return Result.success(Component.translatable(
                String.format("%s.command.zone.filter_item_removed", VillageTale.MOD_ID), itemId, zone.getName()));
    }

    public static Result clearFilter(ServerLevel level, UUID villageId, UUID zoneId) {
        IVillageCapability capability = getVillageCapability(level, villageId);
        if (capability == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.village_capability_not_found", VillageTale.MOD_ID)));
        }

        IVillageZone zone = capability.getZones().stream()
                .filter(z -> z.getUUID().equals(zoneId))
                .findFirst()
                .orElse(null);

        if (zone == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.not_found", VillageTale.MOD_ID), zoneId));
        }

        zone.setFilter(new ArrayList<>());

        return Result.success(Component.translatable(
                String.format("%s.command.zone.filter_cleared", VillageTale.MOD_ID), zone.getName()));
    }

    public static Result listFilter(ServerLevel level, UUID villageId, UUID zoneId) {
        IVillageCapability capability = getVillageCapability(level, villageId);
        if (capability == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.village_capability_not_found", VillageTale.MOD_ID)));
        }

        IVillageZone zone = capability.getZones().stream()
                .filter(z -> z.getUUID().equals(zoneId))
                .findFirst()
                .orElse(null);

        if (zone == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.not_found", VillageTale.MOD_ID), zoneId));
        }

        List<ItemStack> wantedItems = zone.getFilter();

        if (wantedItems.isEmpty()) {
            return Result.success(Component.translatable(
                    String.format("%s.command.zone.no_filter_items", VillageTale.MOD_ID), zone.getName()));
        }

        // Build a list of item names
        List<String> itemNames = new ArrayList<>();
        for (ItemStack stack : wantedItems) {
            itemNames.add(stack.getItem().builtInRegistryHolder().key().location().toString());
        }

        String itemList = String.join(", ", itemNames);
        return Result.success(Component.translatable(
                String.format("%s.command.zone.filter_list", VillageTale.MOD_ID),
                zone.getName(), itemList));
    }

    public static Result addEntityTypeFilter(ServerLevel level, UUID villageId, UUID zoneId, ResourceLocation entityTypeId) {
        IVillageCapability capability = getVillageCapability(level, villageId);
        if (capability == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.village_capability_not_found", VillageTale.MOD_ID)));
        }

        IVillageZone zone = capability.getZones().stream()
                .filter(z -> z.getUUID().equals(zoneId))
                .findFirst()
                .orElse(null);

        if (zone == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.not_found", VillageTale.MOD_ID), zoneId));
        }

        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityTypeId);
        if (entityType == EntityType.PIG && entityTypeId.getPath().equals("pig")) {
            // Default pig case - valid
        } else if (!BuiltInRegistries.ENTITY_TYPE.containsKey(entityTypeId)) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.invalid_entity_type", VillageTale.MOD_ID), entityTypeId));
        }

        if (!(zone.getType() instanceof AbstractZoneType abstractZoneType)) {
            Set<ResourceLocation> currentTypes = zone.getEntityTypeFilter();
            if (currentTypes.contains(entityTypeId)) {
                return Result.failure(Component.translatable(
                        String.format("%s.command.zone.entity_type_already_exists", VillageTale.MOD_ID), entityTypeId));
            }

            Set<ResourceLocation> updatedTypes = new HashSet<>(currentTypes);
            updatedTypes.add(entityTypeId);
            zone.setEntityTypeFilter(updatedTypes);

            return Result.success(Component.translatable(
                    String.format("%s.command.zone.entity_type_added", VillageTale.MOD_ID), entityTypeId, zone.getName()));
        }

        Set<ResourceLocation> validFilter = abstractZoneType.getEntityFilter();
        if (!validFilter.isEmpty() && !validFilter.contains(entityTypeId)) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.invalid_entity_type_for_zone", VillageTale.MOD_ID),
                    entityTypeId, zone.getType().getID()));
        }

        Set<ResourceLocation> currentTypes = zone.getEntityTypeFilter();
        if (currentTypes.contains(entityTypeId)) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.entity_type_already_exists", VillageTale.MOD_ID), entityTypeId));
        }

        Set<ResourceLocation> updatedTypes = new HashSet<>(currentTypes);
        updatedTypes.add(entityTypeId);
        zone.setEntityTypeFilter(updatedTypes);

        return Result.success(Component.translatable(
                String.format("%s.command.zone.entity_type_added", VillageTale.MOD_ID), entityTypeId, zone.getName()));
    }

    public static Result removeEntityTypeFilter(ServerLevel level, UUID villageId, UUID zoneId, ResourceLocation entityTypeId) {
        IVillageCapability capability = getVillageCapability(level, villageId);
        if (capability == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.village_capability_not_found", VillageTale.MOD_ID)));
        }

        IVillageZone zone = capability.getZones().stream()
                .filter(z -> z.getUUID().equals(zoneId))
                .findFirst()
                .orElse(null);

        if (zone == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.not_found", VillageTale.MOD_ID), zoneId));
        }

        Set<ResourceLocation> currentTypes = zone.getEntityTypeFilter();
        if (!currentTypes.contains(entityTypeId)) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.entity_type_not_found", VillageTale.MOD_ID), entityTypeId));
        }

        Set<ResourceLocation> updatedTypes = new HashSet<>(currentTypes);
        updatedTypes.remove(entityTypeId);
        zone.setEntityTypeFilter(updatedTypes);

        return Result.success(Component.translatable(
                String.format("%s.command.zone.entity_type_removed", VillageTale.MOD_ID), entityTypeId, zone.getName()));
    }

    public static Result clearEntityTypeFilter(ServerLevel level, UUID villageId, UUID zoneId) {
        IVillageCapability capability = getVillageCapability(level, villageId);
        if (capability == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.village_capability_not_found", VillageTale.MOD_ID)));
        }

        IVillageZone zone = capability.getZones().stream()
                .filter(z -> z.getUUID().equals(zoneId))
                .findFirst()
                .orElse(null);

        if (zone == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.not_found", VillageTale.MOD_ID), zoneId));
        }

        zone.setEntityTypeFilter(new HashSet<>());

        return Result.success(Component.translatable(
                String.format("%s.command.zone.entity_type_filter_cleared", VillageTale.MOD_ID), zone.getName()));
    }

    public static Result listEntityTypeFilter(ServerLevel level, UUID villageId, UUID zoneId) {
        IVillageCapability capability = getVillageCapability(level, villageId);
        if (capability == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.village_capability_not_found", VillageTale.MOD_ID)));
        }

        IVillageZone zone = capability.getZones().stream()
                .filter(z -> z.getUUID().equals(zoneId))
                .findFirst()
                .orElse(null);

        if (zone == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.not_found", VillageTale.MOD_ID), zoneId));
        }

        Set<ResourceLocation> entityTypes = zone.getEntityTypeFilter();

        if (entityTypes.isEmpty()) {
            return Result.success(Component.translatable(
                    String.format("%s.command.zone.no_entity_types", VillageTale.MOD_ID), zone.getName()));
        }

        // Build a list of entity type names
        List<String> entityTypeNames = new ArrayList<>();
        for (ResourceLocation entityTypeId : entityTypes) {
            entityTypeNames.add(entityTypeId.toString());
        }

        String entityTypeList = String.join(", ", entityTypeNames);
        return Result.success(Component.translatable(
                String.format("%s.command.zone.entity_type_list", VillageTale.MOD_ID),
                zone.getName(), entityTypeList));
    }
}
