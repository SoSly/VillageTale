package org.sosly.villageworks.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.sosly.villageworks.api.capability.IVillagesCapability;
import org.sosly.villageworks.api.IProfession;
import org.sosly.villageworks.capability.Capabilities;
import org.sosly.villageworks.command.arguments.VillageUUIDArgument;
import org.sosly.villageworks.data.LivingEntityFoodData;
import org.sosly.villageworks.data.VillageInfo;
import org.sosly.villageworks.entity.Villager;
import org.sosly.villageworks.profession.ProfessionRegistry;

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
                        .executes(VillagerCommand::addExhaustion)))));
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
}
