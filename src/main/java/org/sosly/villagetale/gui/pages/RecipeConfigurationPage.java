package org.sosly.villagetale.gui.pages;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.crafting.Recipe;
import org.sosly.villagetale.api.IProfession;
import org.sosly.villagetale.client.ClientDataManager;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.gui.LedgerScreen;
import org.sosly.villagetale.gui.components.CompactCheckbox;
import org.sosly.villagetale.gui.components.LedgerIconButton;
import org.sosly.villagetale.network.packets.serverbound.UpdateVillagerRecipes;
import org.sosly.villagetale.profession.ProfessionRegistry;

public class RecipeConfigurationPage extends AbstractLedgerPage {
    private static final int LIST_TOP = 36;
    private static final int LIST_BOTTOM = 145;

    private final int villagerEntityId;
    private RecipeList recipeList;
    private LedgerIconButton backButton;
    private Set<ResourceLocation> selectedRecipes;

    public RecipeConfigurationPage(LedgerScreen screen, UUID villageId, int villagerEntityId) {
        super(screen, villageId);
        this.villagerEntityId = villagerEntityId;
        this.selectedRecipes = new HashSet<>();
    }

    @Override
    public void attach(int uStart, int vStart) {
        super.attach(uStart, vStart);

        Set<ResourceLocation> cachedRecipes = ClientDataManager.getCachedRecipes(villagerEntityId);
        if (cachedRecipes != null) {
            selectedRecipes.addAll(cachedRecipes);
        }

        Villager villager = getVillager();
        if (villager == null) {
            return;
        }

        ResourceLocation professionId = ClientDataManager.getCachedProfession(villagerEntityId);
        if (professionId == null) {
            return;
        }

        IProfession profession = ProfessionRegistry.INSTANCE.getProfession(professionId).orElse(null);
        if (profession == null) {
            return;
        }

        List<RecipeEntry> entries = new ArrayList<>();
        Minecraft mc = screen.getMinecraft();
        if (mc != null && mc.level != null) {
            mc.level.getRecipeManager().getRecipes().forEach(recipe -> {
                if (profession.getLearnableItems().matches(recipe.getResultItem(mc.level.registryAccess()))) {
                    ResourceLocation recipeId = recipe.getId();
                    String displayName = recipe.getResultItem(mc.level.registryAccess()).getDisplayName().getString();
                    entries.add(new RecipeEntry(recipeId, displayName));
                }
            });
        }

        entries.sort((a, b) -> a.displayName.compareTo(b.displayName));

        this.recipeList = new RecipeList(
            screen.getMinecraft(),
            LedgerScreen.CONTENT_WIDTH,
            LIST_BOTTOM - LIST_TOP,
            vStart + LIST_TOP,
            vStart + LIST_BOTTOM,
            12,
            entries
        );
        this.recipeList.setLeftPos(uStart);
        addWidget(this.recipeList);

        this.backButton = LedgerIconButton.Back(
            uStart + (LedgerScreen.CONTENT_WIDTH - 14) / 2,
            vStart + 153,
            button -> closeRecipes(),
            Component.translatable("villagetale.gui.back")
        );
        addRenderableWidget(this.backButton);
    }

    @Override
    public void detach() {
        super.detach();
        this.recipeList = null;
        this.backButton = null;
        this.selectedRecipes.clear();
    }

    private void closeRecipes() {
        Villager villager = getVillager();
        float health = villager != null ? villager.getHealth() : 0;
        int hunger = villager != null ? villager.getFoodData().getFoodLevel() : 0;

        screen.setLeftPage(new VillagerManagementPage(screen, villagerEntityId, villageId, null, null));
        screen.setRightPage(new VillagerStatsPage(screen, villagerEntityId, villageId, health, hunger));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Component title = Component.translatable("villagetale.gui.recipe.configure_recipes");
        guiGraphics.drawString(font, title, uStart, vStart + 16, 0, false);

        if (this.recipeList != null) {
            this.recipeList.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    private void sendRecipeUpdate() {
        UpdateVillagerRecipes.send(villagerEntityId, new HashSet<>(selectedRecipes));
    }

    private Villager getVillager() {
        if (screen.getMinecraft() == null || screen.getMinecraft().level == null) {
            return null;
        }

        Entity entity = screen.getMinecraft().level.getEntity(villagerEntityId);
        if (entity instanceof Villager villager) {
            return villager;
        }

        return null;
    }

    private record RecipeEntry(ResourceLocation id, String displayName) {}

    private class RecipeList extends ObjectSelectionList<RecipeList.Entry> {
        private final List<RecipeEntry> recipeEntries;

        public RecipeList(Minecraft minecraft, int width, int height, int y, int bottom, int itemHeight, List<RecipeEntry> entries) {
            super(minecraft, width, height, y, bottom, itemHeight);
            this.recipeEntries = entries;
            this.setRenderBackground(false);
            this.setRenderTopAndBottom(false);
            this.setRenderSelection(false);
            for (RecipeEntry entry : entries) {
                this.addEntry(new Entry(entry));
            }
        }

        @Override
        public int getRowWidth() {
            return LedgerScreen.CONTENT_WIDTH;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getRowLeft() + this.getRowWidth() - 6;
        }

        public class Entry extends ObjectSelectionList.Entry<Entry> {
            private final RecipeEntry recipeEntry;
            private CompactCheckbox checkbox;

            public Entry(RecipeEntry recipeEntry) {
                this.recipeEntry = recipeEntry;
                this.checkbox = new CompactCheckbox(
                    0,
                    0,
                    LedgerScreen.CONTENT_WIDTH - 10,
                    Component.literal(recipeEntry.displayName),
                    selectedRecipes.contains(recipeEntry.id)
                ) {
                    @Override
                    public void onPress() {
                        super.onPress();
                        onCheckboxChanged(this.selected());
                    }
                };
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
                this.checkbox.setX(left);
                this.checkbox.setY(top);
                this.checkbox.render(guiGraphics, mouseX, mouseY, partialTick);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return this.checkbox.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public Component getNarration() {
                return Component.literal(recipeEntry.displayName);
            }

            private void onCheckboxChanged(boolean checked) {
                if (checked) {
                    selectedRecipes.add(recipeEntry.id);
                } else {
                    selectedRecipes.remove(recipeEntry.id);
                }
                sendRecipeUpdate();
            }
        }
    }
}
