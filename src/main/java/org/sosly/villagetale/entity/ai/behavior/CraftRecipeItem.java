package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.api.IRecipeManager;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.compat.CompatRegistry;
import org.sosly.villagetale.config.CommonConfig;
import org.sosly.villagetale.data.CraftingMethod;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.ContainerHelper;
import org.sosly.villagetale.helper.VillagerHelper;
import org.sosly.villagetale.helper.VillagesHelper;

public class CraftRecipeItem extends Behavior<Villager> {
    private static final int BEHAVIOR_DURATION = 200;
    private static final int CRAFTING_DURATION = 60;
    private boolean claimed;
    private Recipe<?> recipe;
    private BlockPos workstation;
    private IVillageZone workplace;
    private CraftingMethod craftingMethod;
    private int craftingTicks;

    public CraftRecipeItem() {
        super(ImmutableMap.of(
                MemoryModuleTypes.WORK_ZONE.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.NEAREST_WORKSTATION.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.CURRENT_RECIPE.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.BUSY.get(), MemoryStatus.VALUE_ABSENT
        ), BEHAVIOR_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel level, @NotNull Villager villager) {
        IVillageZone zone = VillagesHelper.getWorkplaceZone(level, villager);
        if (zone == null) {
            return false;
        }

        if (!zone.containsPosition(villager.blockPosition())) {
            return false;
        }

        Recipe<?> recipe = VillagerHelper.getCurrentRecipe(level, villager);
        if (recipe == null) {
            return false;
        }

        BlockPos pos = villager.getBrain().getMemory(MemoryModuleTypes.NEAREST_WORKSTATION.get())
                .orElse(null);
        if (pos == null) {
            return false;
        }

        this.craftingTicks = 0;
        this.recipe = recipe;
        this.workstation = pos;
        this.workplace = zone;
        this.craftingMethod = CompatRegistry.getRecipeManager().getCraftingMethod(recipe);
        return true;
    }

    @Override
    protected void start(@NotNull ServerLevel level, Villager villager, long gameTime) {
        claimed = workplace.claim(workstation, villager.getUUID(), BEHAVIOR_DURATION, gameTime);
        if (!claimed) {
            return;
        }

        villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.BUSY.get(), true, BEHAVIOR_DURATION);

        if (villager.blockPosition().closerThan(workstation, CommonConfig.interactionDistance)) {
            return;
        }

        villager.getBrain().setMemoryWithExpiry(MemoryModuleType.WALK_TARGET,
                new WalkTarget(workstation, 0.5f, 1), 200L);
    }

    @Override
    protected void stop(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        villager.getBrain().eraseMemory(MemoryModuleTypes.BUSY.get());

        if (this.craftingMethod != CraftingMethod.CONTAINER) {
            villager.getBrain().eraseMemory(MemoryModuleTypes.NEAREST_WORKSTATION.get());
            villager.getBrain().eraseMemory(MemoryModuleTypes.CURRENT_RECIPE.get());
        }

        if (this.claimed && this.workplace != null && this.workstation != null) {
            this.workplace.release(this.workstation);
        }

        this.claimed = false;
        this.recipe = null;
        this.workstation = null;
        this.workplace = null;
        this.craftingMethod = null;
    }

    @Override
    protected boolean canStillUse(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        return claimed;
    }

    @Override
    protected void tick(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        if (!claimed || workstation == null) {
            return;
        }

        if (!villager.blockPosition().closerThan(workstation, CommonConfig.interactionDistance)) {
            return;
        }

        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);

        switch (craftingMethod) {
            case FAKE -> handleFakeCrafting(level, villager);
            case CONTAINER -> handleContainerCrafting(level, villager);
            case INTERACTION -> handleInteractionCrafting(level, villager);
        }
    }

    private void handleFakeCrafting(ServerLevel level, Villager villager) {
        if (craftingTicks++ < CRAFTING_DURATION) {
            if (craftingTicks % 10 == 0) {
                villager.getLookControl().setLookAt(
                    workstation.getX() + 0.5,
                    workstation.getY() + 0.5,
                    workstation.getZ() + 0.5
                );
                villager.swing(InteractionHand.MAIN_HAND);

                IRecipeManager recipeManager = CompatRegistry.getRecipeManager();
                ResourceLocation soundLocation = recipeManager.getCraftingSound(recipe).orElse(null);
                if (soundLocation != null) {
                    level.playSound(null, workstation, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }
            return;
        }

        craftItemFake(level, villager);
    }

    private void handleContainerCrafting(ServerLevel level, Villager villager) {
        IRecipeManager recipeManager = CompatRegistry.getRecipeManager();
        SimpleContainer inventory = villager.getInventory();

        int[] inputSlots = recipeManager.getInputSlots(recipe);
        if (inputSlots.length == 0) {
            return;
        }

        if (!hasAllIngredients(inventory)) {
            return;
        }

        ContainerHelper.openContainer(level, workstation);

        for (int slot : inputSlots) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.isEmpty()) {
                    continue;
                }

                ItemStack ingredientStack = findIngredientInInventory(inventory, ingredient);
                if (ingredientStack.isEmpty()) {
                    continue;
                }

                ItemStack toPlace = ingredientStack.copy();
                toPlace.setCount(1);

                if (ContainerHelper.placeItemInSlot(level, workstation, slot, toPlace)) {
                    ingredientStack.shrink(1);
                    break;
                }
            }
        }

        if (villager.getBrain().hasMemoryValue(MemoryModuleTypes.WORKSTATION_NEEDS_FUEL.get())) {
            int fuelSlot = recipeManager.getFuelSlot(recipe).orElse(-1);
            if (fuelSlot >= 0) {
                placeFuelInSlot(level, villager, fuelSlot, 3);
            }
        }

        workplace.release(workstation);
        this.claimed = false;
    }

    private void handleInteractionCrafting(ServerLevel level, Villager villager) {
        SimpleContainer inventory = villager.getInventory();

        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) {
                continue;
            }

            ItemStack ingredientStack = findIngredientInInventory(inventory, ingredient);
            if (ingredientStack.isEmpty()) {
                continue;
            }

            FakePlayer fakePlayer = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "[VillageTale]"));
            fakePlayer.setPos(villager.getX(), villager.getY(), villager.getZ());
            fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ingredientStack.copy());

            BlockState state = level.getBlockState(workstation);
            state.use(level, fakePlayer, InteractionHand.MAIN_HAND,
                level.clip(new net.minecraft.world.level.ClipContext(
                    fakePlayer.getEyePosition(),
                    workstation.getCenter(),
                    net.minecraft.world.level.ClipContext.Block.OUTLINE,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    fakePlayer
                )));

            ingredientStack.shrink(1);
            break;
        }

        workplace.release(workstation);
        this.claimed = false;
    }

    private void craftItemFake(@NotNull ServerLevel level, @NotNull Villager villager) {
        SimpleContainer inventory = villager.getInventory();

        if (!hasAllIngredients(inventory)) {
            return;
        }

        consumeIngredients(inventory);
        produceResult(level, villager, inventory);
    }

    private ItemStack findIngredientInInventory(SimpleContainer inventory, Ingredient ingredient) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && ingredient.test(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private void placeFuelInSlot(ServerLevel level, Villager villager, int fuelSlot, int maxFuelToPlace) {
        IRecipeManager recipeManager = CompatRegistry.getRecipeManager();
        SimpleContainer inventory = villager.getInventory();

        recipeManager.getFuelItems(recipe).ifPresent(fuelMatcher -> {
            int placed = 0;
            for (int i = 0; i < inventory.getContainerSize() && placed < maxFuelToPlace; i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack.isEmpty()) {
                    continue;
                }

                if (fuelMatcher.matches(stack)) {
                    int toPlace = Math.min(stack.getCount(), maxFuelToPlace - placed);
                    ItemStack fuelStack = stack.copy();
                    fuelStack.setCount(toPlace);

                    if (ContainerHelper.placeItemInSlot(level, workstation, fuelSlot, fuelStack)) {
                        stack.shrink(toPlace);
                        placed += toPlace;
                    }
                }
            }
        });
    }

    private boolean hasAllIngredients(SimpleContainer inventory) {
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) {
                continue;
            }

            if (!hasIngredient(inventory, ingredient)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasIngredient(SimpleContainer inventory, Ingredient ingredient) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && ingredient.test(stack)) {
                return true;
            }
        }
        return false;
    }

    private void consumeIngredients(SimpleContainer inventory) {
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) {
                continue;
            }

            consumeIngredient(inventory, ingredient);
        }
    }

    private void consumeIngredient(SimpleContainer inventory, Ingredient ingredient) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !ingredient.test(stack)) {
                continue;
            }

            stack.shrink(1);
            inventory.setItem(i, stack);
            return;
        }
    }

    private void produceResult(ServerLevel level, Villager villager, SimpleContainer inventory) {
        ItemStack result = recipe.getResultItem(level.registryAccess());
        if (result.isEmpty()) {
            return;
        }

        ItemStack toAdd = result.copy();
        if (!inventory.canAddItem(toAdd)) {
            villager.spawnAtLocation(toAdd);
            return;
        }

        inventory.addItem(toAdd);
    }
}
