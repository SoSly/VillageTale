package org.sosly.villagetale.command.villager;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IProfession;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.command.Result;
import org.sosly.villagetale.data.LivingEntityFoodData;
import org.sosly.villagetale.data.VillageInfo;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.profession.ProfessionRegistry;

public class VillagerService {

    public static Result queryVillageAssignment(Villager villager) {
        Optional<UUID> villageId = villager.getVillage();
        if (villageId.isPresent()) {
            return Result.success(Component.literal(String.format("Villager %s is assigned to village %s",
                    villager.getDisplayName().getString(), villageId.get())));
        } else {
            return Result.success(Component.literal(String.format("Villager %s is not assigned to any village",
                    villager.getDisplayName().getString())));
        }
    }

    public static Result queryVillageAssignment(Collection<? extends Entity> entities) {
        int checkedCount = 0;
        StringBuilder message = new StringBuilder();

        for (Entity entity : entities) {
            if (!(entity instanceof Villager villager)) {
                continue;
            }

            Optional<UUID> villageId = villager.getVillage();
            if (villageId.isPresent()) {
                message.append(String.format("Villager %s is assigned to village %s\n",
                        villager.getDisplayName().getString(), villageId.get()));
            } else {
                message.append(String.format("Villager %s is not assigned to any village\n",
                        villager.getDisplayName().getString()));
            }
            checkedCount++;
        }

        if (checkedCount == 0) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.no_villagers", VillageTale.MOD_ID)));
        }

        return Result.success(Component.literal(message.toString().trim()));
    }

    public static Result assignVillage(ServerLevel level, Villager villager, UUID villageId) {
        IVillagesCapability villages = level.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
        if (villages == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.capability_not_found", VillageTale.MOD_ID)));
        }

        VillageInfo village = villages.getVillageById(villageId);
        if (village == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.village_not_found", VillageTale.MOD_ID), villageId));
        }

        villager.setVillage(villageId);
        return Result.success(Component.translatable(
                String.format("%s.command.villager.assigned", VillageTale.MOD_ID), 1, village.getVillageName()));
    }

    public static Result assignVillage(ServerLevel level, Collection<? extends Entity> entities, UUID villageId) {
        IVillagesCapability villages = level.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
        if (villages == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.capability_not_found", VillageTale.MOD_ID)));
        }

        VillageInfo village = villages.getVillageById(villageId);
        if (village == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.village_not_found", VillageTale.MOD_ID), villageId));
        }

        int assignedCount = 0;
        for (Entity entity : entities) {
            if (entity instanceof Villager villager) {
                villager.setVillage(villageId);
                assignedCount++;
            }
        }

        if (assignedCount == 0) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.no_villagers", VillageTale.MOD_ID)));
        }

        return Result.success(Component.translatable(
                String.format("%s.command.villager.assigned", VillageTale.MOD_ID), assignedCount, village.getVillageName()));
    }

    public static Result queryProfession(Villager villager) {
        IProfession profession = villager.getProfession();
        return Result.success(Component.literal(String.format("Villager %s has profession: %s",
                villager.getDisplayName().getString(), profession.getID())));
    }

    public static Result queryProfession(Collection<? extends Entity> entities) {
        int checkedCount = 0;
        StringBuilder message = new StringBuilder();

        for (Entity entity : entities) {
            if (!(entity instanceof Villager villager)) {
                continue;
            }

            IProfession profession = villager.getProfession();
            message.append(String.format("Villager %s has profession: %s\n",
                    villager.getDisplayName().getString(), profession.getID()));
            checkedCount++;
        }

        if (checkedCount == 0) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.no_villagers", VillageTale.MOD_ID)));
        }

        return Result.success(Component.literal(message.toString().trim()));
    }

    public static Result setProfession(Villager villager, ResourceLocation professionId) {
        Optional<IProfession> professionOpt = ProfessionRegistry.INSTANCE.getProfession(professionId);
        if (professionOpt.isEmpty()) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.unknown_profession", VillageTale.MOD_ID), professionId));
        }

        IProfession profession = professionOpt.get();
        villager.setProfession(profession.getID());
        return Result.success(Component.translatable(
                String.format("%s.command.villager.profession_set", VillageTale.MOD_ID), 1, professionId));
    }

    public static Result setProfession(Collection<? extends Entity> entities, ResourceLocation professionId) {
        Optional<IProfession> professionOpt = ProfessionRegistry.INSTANCE.getProfession(professionId);
        if (professionOpt.isEmpty()) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.unknown_profession", VillageTale.MOD_ID), professionId));
        }

        IProfession profession = professionOpt.get();
        int changedCount = 0;

        for (Entity entity : entities) {
            if (entity instanceof Villager villager) {
                villager.setProfession(profession.getID());
                changedCount++;
            }
        }

        if (changedCount == 0) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.no_villagers", VillageTale.MOD_ID)));
        }

        return Result.success(Component.translatable(
                String.format("%s.command.villager.profession_set", VillageTale.MOD_ID), changedCount, professionId));
    }

    public static Result displayHunger(Villager villager) {
        LivingEntityFoodData foodData = villager.getFoodData();
        return Result.success(Component.literal(String.format(
                "Villager %s: Hunger %d/20, Saturation %.1f, Exhaustion %.1f",
                villager.getDisplayName().getString(),
                foodData.getFoodLevel(),
                foodData.getSaturationLevel(),
                foodData.getExhaustionLevel()
        )));
    }

    public static Result displayHunger(Collection<? extends Entity> entities) {
        int checkedCount = 0;
        StringBuilder message = new StringBuilder();

        for (Entity entity : entities) {
            if (!(entity instanceof Villager villager)) {
                continue;
            }

            LivingEntityFoodData foodData = villager.getFoodData();
            message.append(String.format("Villager %s: Food=%d/20, Saturation=%.1f, Exhaustion=%.1f\n",
                    villager.getDisplayName().getString(),
                    foodData.getFoodLevel(),
                    foodData.getSaturationLevel(),
                    foodData.getExhaustionLevel()));
            checkedCount++;
        }

        if (checkedCount == 0) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.no_villagers", VillageTale.MOD_ID)));
        }

        return Result.success(Component.literal(message.toString().trim()));
    }

    public static Result addExhaustion(Villager villager, float amount) {
        villager.getFoodData().addExhaustion(amount);
        return Result.success(Component.translatable(
                String.format("%s.command.villager.exhaustion_added", VillageTale.MOD_ID), amount, 1));
    }

    public static Result addExhaustion(Collection<? extends Entity> entities, float amount) {
        int exhaustedCount = 0;

        for (Entity entity : entities) {
            if (entity instanceof Villager villager) {
                villager.getFoodData().addExhaustion(amount);
                exhaustedCount++;
            }
        }

        if (exhaustedCount == 0) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.no_villagers", VillageTale.MOD_ID)));
        }

        return Result.success(Component.translatable(
                String.format("%s.command.villager.exhaustion_added", VillageTale.MOD_ID), amount, exhaustedCount));
    }

    public static void displayDetailedInfo(Villager villager, ServerLevel level, Component header,
                                           java.util.function.Consumer<Component> sender) {
        sender.accept(header);
        sender.accept(Component.literal(String.format("Name: %s", villager.getDisplayName().getString())));
        sender.accept(Component.literal(String.format("UUID: %s", villager.getUUID())));
        sender.accept(Component.literal(String.format("Profession: %s", villager.getProfession().getID())));

        Optional<UUID> villageId = villager.getVillage();
        if (villageId.isPresent()) {
            IVillagesCapability villages = level.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
            if (villages != null) {
                VillageInfo village = villages.getVillageById(villageId.get());
                if (village != null) {
                    sender.accept(Component.literal(String.format("Village: %s (%s)",
                            village.getVillageName(), villageId.get())));
                }
            }
        } else {
            sender.accept(Component.literal("Village: None"));
        }

        sender.accept(Component.literal(""));
        sender.accept(Component.literal("--- Hunger ---"));
        LivingEntityFoodData foodData = villager.getFoodData();
        sender.accept(Component.literal(String.format("Food Level: %d/20", foodData.getFoodLevel())));
        sender.accept(Component.literal(String.format("Saturation: %.1f", foodData.getSaturationLevel())));
        sender.accept(Component.literal(String.format("Exhaustion: %.1f", foodData.getExhaustionLevel())));

        sender.accept(Component.literal(""));
        sender.accept(Component.literal("--- Brain State ---"));
        sender.accept(Component.literal(String.format("Current Activity: %s", 
                villager.getBrain().getActiveNonCoreActivity().orElse(null))));
        sender.accept(Component.literal(String.format("Schedule Activity: %s at time %d", 
                villager.getBrain().getSchedule().getActivityAt((int)(level.getDayTime() % 24000)),
                level.getDayTime() % 24000)));
        
        String runningBehaviors = villager.getBrain().getRunningBehaviors().stream()
                .map(behavior -> {
                    String name = behavior.getClass().getSimpleName();
                    if (name.equals("RunOne")) {
                        return name + "[check debugger]";
                    }
                    return name;
                })
                .collect(java.util.stream.Collectors.joining(", "));
        if (!runningBehaviors.isEmpty()) {
            sender.accept(Component.literal(String.format("Running Behaviors: %s", runningBehaviors)));
        } else {
            sender.accept(Component.literal("Running Behaviors: None"));
        }
        
        sender.accept(Component.literal(""));
        sender.accept(Component.literal("--- Core Memories ---"));

        villager.getBrain().getMemory(MemoryModuleType.HOME).ifPresent(home -> {
            BlockPos pos = home.pos();
            sender.accept(Component.literal(String.format("Home Bed: %d, %d, %d",
                    pos.getX(), pos.getY(), pos.getZ())));
        });

        villager.getBrain().getMemory(MemoryModuleType.LAST_SLEPT).ifPresent(lastSlept -> {
            sender.accept(Component.literal(String.format("Last Slept: %d ticks ago",
                    villager.level().getGameTime() - lastSlept)));
        });

        villager.getBrain().getMemory(MemoryModuleTypes.HOME_ZONE.get()).ifPresent(zoneId -> {
            sender.accept(Component.literal(String.format("Home Zone: %s", zoneId)));
        });

        villager.getBrain().getMemory(MemoryModuleTypes.WORK_ZONE.get()).ifPresent(zoneId -> {
            sender.accept(Component.literal(String.format("Work Zone: %s", zoneId)));
        });

        sender.accept(Component.literal(""));
        sender.accept(Component.literal("--- Transient Memories ---"));

        villager.getBrain().getMemory(MemoryModuleTypes.WANTED_ITEM.get()).ifPresent(item -> {
            String predicateStr = item.getMatcher().toString();
            String shortPredicate = predicateStr.contains("$$Lambda")
                    ? "lambda predicate"
                    : predicateStr.substring(Math.max(0, predicateStr.lastIndexOf('.') + 1));
            sender.accept(Component.literal(String.format("Wanted Item: amount=%d, minimum=%d, matcher=%s",
                    item.getAmount(), item.getMinimum(), shortPredicate)));
        });

        villager.getBrain().getMemory(MemoryModuleTypes.FOUND_ITEM.get()).ifPresent(item -> {
            BlockPos pos = item.containerPos();
            sender.accept(Component.literal(String.format("Found Item: %s at container %d, %d, %d",
                    item.itemId(), pos.getX(), pos.getY(), pos.getZ())));
        });

        villager.getBrain().getMemory(MemoryModuleTypes.ITEMS_TO_DEPOSIT.get()).ifPresent(items -> {
            if (!items.isEmpty()) {
                sender.accept(Component.literal("Items to Deposit:"));
                for (Map.Entry<ResourceLocation, Integer> entry : items.entrySet()) {
                    sender.accept(Component.literal(String.format("  - %s: %d",
                            entry.getKey(), entry.getValue())));
                }
            }
        });

        villager.getBrain().getMemory(MemoryModuleTypes.IS_HUNGRY.get()).ifPresent(hungry -> {
            sender.accept(Component.literal(String.format("Is Hungry: %s", hungry)));
        });

        villager.getBrain().getMemory(MemoryModuleTypes.IS_STARVING.get()).ifPresent(starving -> {
            sender.accept(Component.literal(String.format("Is Starving: %s", starving)));
        });

        villager.getBrain().getMemory(MemoryModuleTypes.CAN_EAT.get()).ifPresent(canEat -> {
            sender.accept(Component.literal(String.format("Can Eat: %s", canEat)));
        });

        villager.getBrain().getMemory(MemoryModuleTypes.BUSY.get()).ifPresent(busy -> {
            sender.accept(Component.literal(String.format("Busy: %s", busy)));
        });

        IProfession profession = villager.getProfession();
        if (!profession.getID().equals(new ResourceLocation("villagetale", "commoner"))) {
            sender.accept(Component.literal(""));
            sender.accept(Component.literal("--- Profession Memories ---"));

            if (profession.getID().equals(new ResourceLocation("villagetale", "farmer"))) {
                villager.getBrain().getMemory(MemoryModuleTypes.NEAREST_TILLABLE_SOIL.get()).ifPresent(pos -> {
                    sender.accept(Component.literal(String.format("Nearest Tillable Soil: %d, %d, %d",
                            pos.getX(), pos.getY(), pos.getZ())));
                });

                villager.getBrain().getMemory(MemoryModuleTypes.NEAREST_EMPTY_FARMLAND.get()).ifPresent(pos -> {
                    sender.accept(Component.literal(String.format("Nearest Empty Farmland: %d, %d, %d",
                            pos.getX(), pos.getY(), pos.getZ())));
                });

                villager.getBrain().getMemory(MemoryModuleTypes.NEAREST_HARVESTABLE_CROP.get()).ifPresent(pos -> {
                    sender.accept(Component.literal(String.format("Nearest Harvestable Crop: %d, %d, %d",
                            pos.getX(), pos.getY(), pos.getZ())));
                });
            }
        }

        sender.accept(Component.literal(""));
        sender.accept(Component.literal("--- Inventory ---"));

        boolean hasItems = false;
        for (int i = 0; i < villager.getInventory().getContainerSize(); i++) {
            ItemStack stack = villager.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                hasItems = true;
                sender.accept(Component.literal(String.format("Slot %d: %s x%d",
                        i, stack.getItem(), stack.getCount())));
            }
        }

        if (!hasItems) {
            sender.accept(Component.literal("Empty"));
        }

        sender.accept(Component.literal("==========================="));
    }
}