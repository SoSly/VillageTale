package org.sosly.villagetale.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import org.sosly.villagetale.api.IProfession;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.command.arguments.VillageUUIDArgument;
import org.sosly.villagetale.data.LivingEntityFoodData;
import org.sosly.villagetale.data.VillageInfo;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.profession.ProfessionRegistry;

public class VillagerCommand {
    private static final SuggestionProvider<CommandSourceStack> PROFESSION_SUGGESTIONS =
        (context, builder) -> SharedSuggestionProvider.suggestResource(
            ProfessionRegistry.INSTANCE.getProfessionIDs().stream(),
            builder
        );

    public static void register(LiteralArgumentBuilder<CommandSourceStack> parentCommand) {
        parentCommand.then(Commands.literal("villager")
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.literal("village")
                    .executes(VillagerCommand::queryVillageAssignment)
                    .then(Commands.argument("villageUUID", VillageUUIDArgument.villageUUID())
                        .suggests((context, builder) -> VillageUUIDArgument.suggest(context, builder))
                        .executes(VillagerCommand::assignVillage)))
                .then(Commands.literal("profession")
                    .executes(VillagerCommand::queryProfession)
                    .then(Commands.argument("profession", ResourceLocationArgument.id())
                        .suggests(PROFESSION_SUGGESTIONS)
                        .executes(VillagerCommand::setProfession)))
                .then(Commands.literal("hunger")
                    .executes(VillagerCommand::displayHunger)
                    .then(Commands.argument("exhaustion", FloatArgumentType.floatArg(0.0F, 40.0F))
                        .executes(VillagerCommand::addExhaustion)))
                .then(Commands.literal("info")
                    .executes(VillagerCommand::displayInfo))));
    }

    private static int queryVillageAssignment(CommandContext<CommandSourceStack> context) {
        try {
            Collection<? extends Entity> entities = EntityArgument.getEntities(context, "targets");
            int checkedCount = 0;

            for (Entity entity : entities) {
                if (entity instanceof Villager villager) {
                    Optional<UUID> villageId = villager.getVillage();

                    if (villageId.isPresent()) {
                        context.getSource().sendSuccess(() ->
                            Component.literal(String.format("Villager %s is assigned to village %s",
                                villager.getDisplayName().getString(),
                                villageId.get())), false);
                    } else {
                        context.getSource().sendSuccess(() ->
                            Component.literal(String.format("Villager %s is not assigned to any village",
                                villager.getDisplayName().getString())), false);
                    }

                    checkedCount++;
                }
            }

            if (checkedCount == 0) {
                context.getSource().sendFailure(Component.literal("No villagers found in selection"));
                return 0;
            }

            return checkedCount;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to query village assignment: " + e.getMessage()));
            return 0;
        }
    }

    private static int assignVillage(CommandContext<CommandSourceStack> context) {
        try {
            Collection<? extends Entity> entities = EntityArgument.getEntities(context, "targets");
            UUID villageId = VillageUUIDArgument.getVillageUUID(context, "villageUUID");
            ServerLevel level = context.getSource().getLevel();

            IVillagesCapability villages = level.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
            if (villages == null) {
                context.getSource().sendFailure(Component.literal("Villages capability not found"));
                return 0;
            }

            VillageInfo village = villages.getVillageById(villageId);
            if (village == null) {
                context.getSource().sendFailure(Component.literal("Village " + villageId + " not found"));
                return 0;
            }

            int assignedCount = 0;
            for (Entity entity : entities) {
                if (entity instanceof Villager villager) {
                    villager.setVillage(villageId);
                    assignedCount++;
                }
            }

            if (assignedCount == 0) {
                context.getSource().sendFailure(Component.literal("No villagers found in selection"));
                return 0;
            }

            final int finalAssignedCount = assignedCount;
            final UUID finalVillageId = villageId;
            context.getSource().sendSuccess(() ->
                Component.literal(String.format("Assigned %d villager(s) to village %s",
                    finalAssignedCount, finalVillageId)), true);

            return assignedCount;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to assign village: " + e.getMessage()));
            return 0;
        }
    }

    private static int queryProfession(CommandContext<CommandSourceStack> context) {
        try {
            Collection<? extends Entity> entities = EntityArgument.getEntities(context, "targets");
            int checkedCount = 0;

            for (Entity entity : entities) {
                if (entity instanceof Villager villager) {
                    IProfession profession = villager.getProfession();

                    context.getSource().sendSuccess(() ->
                        Component.literal(String.format("Villager %s has profession: %s",
                            villager.getDisplayName().getString(),
                            profession.getID())), false);

                    checkedCount++;
                }
            }

            if (checkedCount == 0) {
                context.getSource().sendFailure(Component.literal("No villagers found in selection"));
                return 0;
            }

            return checkedCount;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to query profession: " + e.getMessage()));
            return 0;
        }
    }

    private static int setProfession(CommandContext<CommandSourceStack> context) {
        try {
            Collection<? extends Entity> entities = EntityArgument.getEntities(context, "targets");
            ResourceLocation professionId = ResourceLocationArgument.getId(context, "profession");

            Optional<IProfession> professionOpt = ProfessionRegistry.INSTANCE.getProfession(professionId);
            if (professionOpt.isEmpty()) {
                context.getSource().sendFailure(Component.literal("Unknown profession: " + professionId));
                return 0;
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
                context.getSource().sendFailure(Component.literal("No villagers found in selection"));
                return 0;
            }

            final int finalChangedCount = changedCount;
            final ResourceLocation finalProfessionId = professionId;
            context.getSource().sendSuccess(() ->
                Component.literal(String.format("Set profession of %d villager(s) to %s",
                    finalChangedCount, finalProfessionId)), true);

            return changedCount;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to set profession: " + e.getMessage()));
            return 0;
        }
    }

    private static int displayHunger(CommandContext<CommandSourceStack> context) {
        try {
            Collection<? extends Entity> entities = EntityArgument.getEntities(context, "targets");
            int checkedCount = 0;

            for (Entity entity : entities) {
                if (entity instanceof Villager villager) {
                    LivingEntityFoodData foodData = villager.getFoodData();

                    context.getSource().sendSuccess(() ->
                        Component.literal(String.format("Villager %s: Food=%d/20, Saturation=%.1f, Exhaustion=%.1f",
                            villager.getDisplayName().getString(),
                            foodData.getFoodLevel(),
                            foodData.getSaturationLevel(),
                            foodData.getExhaustionLevel())), false);

                    checkedCount++;
                }
            }

            if (checkedCount == 0) {
                context.getSource().sendFailure(Component.literal("No villagers found in selection"));
                return 0;
            }

            return checkedCount;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to check hunger: " + e.getMessage()));
            return 0;
        }
    }

    private static int addExhaustion(CommandContext<CommandSourceStack> context) {
        try {
            Collection<? extends Entity> entities = EntityArgument.getEntities(context, "targets");
            float amount = FloatArgumentType.getFloat(context, "exhaustion");
            int exhaustedCount = 0;

            for (Entity entity : entities) {
                if (entity instanceof Villager villager) {
                    villager.getFoodData().addExhaustion(amount);
                    exhaustedCount++;
                }
            }

            if (exhaustedCount == 0) {
                context.getSource().sendFailure(Component.literal("No villagers found in selection"));
                return 0;
            }

            final int finalExhaustedCount = exhaustedCount;
            final float finalAmount = amount;
            context.getSource().sendSuccess(() ->
                Component.literal(String.format("Added %.1f exhaustion to %d villager(s)",
                    finalAmount, finalExhaustedCount)), true);

            return exhaustedCount;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to add exhaustion: " + e.getMessage()));
            return 0;
        }
    }

    private static int displayInfo(CommandContext<CommandSourceStack> context) {
        try {
            Collection<? extends Entity> entities = EntityArgument.getEntities(context, "targets");

            for (Entity entity : entities) {
                if (!(entity instanceof Villager villager)) {
                    continue;
                }

                CommandSourceStack source = context.getSource();

                source.sendSuccess(() -> Component.literal("=== Villager Information ==="), false);

                source.sendSuccess(() -> Component.literal(String.format("Name: %s",
                    villager.getDisplayName().getString())), false);
                source.sendSuccess(() -> Component.literal(String.format("UUID: %s",
                    villager.getUUID())), false);
                source.sendSuccess(() -> Component.literal(String.format("Profession: %s",
                    villager.getProfession().getID())), false);

                Optional<UUID> villageId = villager.getVillage();
                if (villageId.isPresent()) {
                    ServerLevel level = source.getLevel();
                    IVillagesCapability villages = level.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
                    if (villages != null) {
                        VillageInfo village = villages.getVillageById(villageId.get());
                        if (village != null) {
                            source.sendSuccess(() -> Component.literal(String.format("Village: %s (%s)",
                                village.getVillageName(), villageId.get())), false);
                        }
                    }
                } else {
                    source.sendSuccess(() -> Component.literal("Village: None"), false);
                }

                source.sendSuccess(() -> Component.literal(""), false);
                source.sendSuccess(() -> Component.literal("--- Hunger ---"), false);
                LivingEntityFoodData foodData = villager.getFoodData();
                source.sendSuccess(() -> Component.literal(String.format("Food Level: %d/20",
                    foodData.getFoodLevel())), false);
                source.sendSuccess(() -> Component.literal(String.format("Saturation: %.1f",
                    foodData.getSaturationLevel())), false);
                source.sendSuccess(() -> Component.literal(String.format("Exhaustion: %.1f",
                    foodData.getExhaustionLevel())), false);

                source.sendSuccess(() -> Component.literal(""), false);
                source.sendSuccess(() -> Component.literal("--- Core Memories ---"), false);

                villager.getBrain().getMemory(MemoryModuleType.HOME).ifPresent(home -> {
                    BlockPos pos = home.pos();
                    source.sendSuccess(() -> Component.literal(String.format("Home Bed: %d, %d, %d",
                        pos.getX(), pos.getY(), pos.getZ())), false);
                });

                villager.getBrain().getMemory(MemoryModuleType.LAST_SLEPT).ifPresent(lastSlept -> {
                    source.sendSuccess(() -> Component.literal(String.format("Last Slept: %d ticks ago",
                        villager.level().getGameTime() - lastSlept)), false);
                });

                villager.getBrain().getMemory(MemoryModuleTypes.HOME_ZONE.get()).ifPresent(zoneId -> {
                    source.sendSuccess(() -> Component.literal(String.format("Home Zone: %s", zoneId)), false);
                });

                villager.getBrain().getMemory(MemoryModuleTypes.WORK_ZONE.get()).ifPresent(zoneId -> {
                    source.sendSuccess(() -> Component.literal(String.format("Work Zone: %s", zoneId)), false);
                });

                source.sendSuccess(() -> Component.literal(""), false);
                source.sendSuccess(() -> Component.literal("--- Transient Memories ---"), false);

                villager.getBrain().getMemory(MemoryModuleTypes.WANTED_ITEM.get()).ifPresent(item -> {
                    String predicateStr = item.getMatcher().toString();
                    String shortPredicate = predicateStr.contains("$$Lambda")
                        ? "lambda predicate"
                        : predicateStr.substring(Math.max(0, predicateStr.lastIndexOf('.') + 1));
                    source.sendSuccess(() -> Component.literal(String.format("Wanted Item: amount=%d, minimum=%d, matcher=%s",
                        item.getAmount(), item.getMinimum(), shortPredicate)), false);
                });

                villager.getBrain().getMemory(MemoryModuleTypes.FOUND_ITEM.get()).ifPresent(item -> {
                    BlockPos pos = item.containerPos();
                    source.sendSuccess(() -> Component.literal(String.format("Found Item: %s at container %d, %d, %d",
                        item.itemId(), pos.getX(), pos.getY(), pos.getZ())), false);
                });

                villager.getBrain().getMemory(MemoryModuleTypes.ITEMS_TO_DEPOSIT.get()).ifPresent(items -> {
                    if (!items.isEmpty()) {
                        source.sendSuccess(() -> Component.literal("Items to Deposit:"), false);
                        for (Map.Entry<ResourceLocation, Integer> entry : items.entrySet()) {
                            source.sendSuccess(() -> Component.literal(String.format("  - %s: %d",
                                entry.getKey(), entry.getValue())), false);
                        }
                    }
                });

                villager.getBrain().getMemory(MemoryModuleTypes.IS_HUNGRY.get()).ifPresent(hungry -> {
                    source.sendSuccess(() -> Component.literal(String.format("Is Hungry: %s", hungry)), false);
                });

                villager.getBrain().getMemory(MemoryModuleTypes.IS_STARVING.get()).ifPresent(starving -> {
                    source.sendSuccess(() -> Component.literal(String.format("Is Starving: %s", starving)), false);
                });

                villager.getBrain().getMemory(MemoryModuleTypes.CAN_EAT.get()).ifPresent(canEat -> {
                    source.sendSuccess(() -> Component.literal(String.format("Can Eat: %s", canEat)), false);
                });

                villager.getBrain().getMemory(MemoryModuleTypes.BUSY.get()).ifPresent(busy -> {
                    source.sendSuccess(() -> Component.literal(String.format("Busy: %s", busy)), false);
                });

                IProfession profession = villager.getProfession();
                if (!profession.getID().equals(new ResourceLocation("villagetale", "commoner"))) {
                    source.sendSuccess(() -> Component.literal(""), false);
                    source.sendSuccess(() -> Component.literal("--- Profession Memories ---"), false);

                    if (profession.getID().equals(new ResourceLocation("villagetale", "farmer"))) {
                        villager.getBrain().getMemory(MemoryModuleTypes.NEAREST_TILLABLE_SOIL.get()).ifPresent(pos -> {
                            source.sendSuccess(() -> Component.literal(String.format("Nearest Tillable Soil: %d, %d, %d",
                                pos.getX(), pos.getY(), pos.getZ())), false);
                        });

                        villager.getBrain().getMemory(MemoryModuleTypes.NEAREST_EMPTY_FARMLAND.get()).ifPresent(pos -> {
                            source.sendSuccess(() -> Component.literal(String.format("Nearest Empty Farmland: %d, %d, %d",
                                pos.getX(), pos.getY(), pos.getZ())), false);
                        });

                        villager.getBrain().getMemory(MemoryModuleTypes.NEAREST_HARVESTABLE_CROP.get()).ifPresent(pos -> {
                            source.sendSuccess(() -> Component.literal(String.format("Nearest Harvestable Crop: %d, %d, %d",
                                pos.getX(), pos.getY(), pos.getZ())), false);
                        });
                    }
                }

                source.sendSuccess(() -> Component.literal(""), false);
                source.sendSuccess(() -> Component.literal("--- Inventory ---"), false);

                boolean hasItems = false;
                for (int i = 0; i < villager.getInventory().getContainerSize(); i++) {
                    ItemStack stack = villager.getInventory().getItem(i);
                    if (!stack.isEmpty()) {
                        hasItems = true;
                        int finalI = i;
                        source.sendSuccess(() -> Component.literal(String.format("Slot %d: %s x%d",
                                finalI, stack.getItem(), stack.getCount())), false);
                    }
                }

                if (!hasItems) {
                    source.sendSuccess(() -> Component.literal("Empty"), false);
                }

                source.sendSuccess(() -> Component.literal("==========================="), false);
            }

            return 1;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to display villager info: " + e.getMessage()));
            return 0;
        }
    }
}
