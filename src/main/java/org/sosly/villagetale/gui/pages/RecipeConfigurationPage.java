package org.sosly.villagetale.gui.pages;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.sosly.villagetale.api.IProfession;
import org.sosly.villagetale.client.ClientDataManager;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.gui.LedgerScreen;
import org.sosly.villagetale.gui.components.CompactCheckbox;
import org.sosly.villagetale.gui.components.LedgerIconButton;
import org.sosly.villagetale.network.packets.serverbound.UpdateVillagerRecipes;
import org.sosly.villagetale.profession.ProfessionRegistry;

public class RecipeConfigurationPage extends AbstractLedgerPage {
    private static final int LIST_TOP = 24;
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

        Map<String, List<RecipeEntry>> recipesByType = new LinkedHashMap<>();
        Minecraft mc = screen.getMinecraft();
        if (mc != null && mc.level != null) {
            mc.level.getRecipeManager().getRecipes().forEach(recipe -> {
                if (profession.getLearnableItems().matches(recipe.getResultItem(mc.level.registryAccess()))) {
                    ResourceLocation recipeId = recipe.getId();
                    Component displayName = recipe.getResultItem(mc.level.registryAccess()).getHoverName();
                    String recipeType = recipe.getType().toString();
                    String friendlyTypeName = getRecipeTypeName(recipeType);

                    recipesByType.computeIfAbsent(friendlyTypeName, k -> new ArrayList<>())
                        .add(new RecipeEntry(recipeId, displayName, friendlyTypeName));
                }
            });
        }

        List<String> sortedTypes = new ArrayList<>(recipesByType.keySet());
        sortedTypes.sort(String::compareTo);

        for (String typeName : sortedTypes) {
            List<RecipeEntry> recipes = recipesByType.get(typeName);
            recipes.sort((a, b) -> a.displayName.getString().compareTo(b.displayName.getString()));
        }

        this.recipeList = new RecipeList(
            screen.getMinecraft(),
            LedgerScreen.CONTENT_WIDTH,
            LIST_BOTTOM - LIST_TOP,
            vStart + LIST_TOP,
            vStart + LIST_BOTTOM,
            12,
            recipesByType,
            sortedTypes
        );
        this.recipeList.setLeftPos(uStart);
        addWidget(this.recipeList);

        this.backButton = LedgerIconButton.back(
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
        int physique = villager != null ? villager.getStats().getPhysique() : 0;
        int endurance = villager != null ? villager.getStats().getEndurance() : 0;
        int intellect = villager != null ? villager.getStats().getIntellect() : 0;

        screen.setLeftPage(new VillagerManagementPage(screen, villagerEntityId, villageId, null, null));
        screen.setRightPage(new VillagerStatsPage(screen, villagerEntityId, villageId, health, hunger, physique, endurance, intellect));
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

    private static String getRecipeTypeName(String recipeTypeString) {
        String cleanType = recipeTypeString;
        if (cleanType.startsWith("RecipeType[")) {
            cleanType = cleanType.substring(11, cleanType.length() - 1);
        }

        if (!cleanType.contains(":")) {
            return capitalizeWords(cleanType.replace("_", " "));
        }

        String namespace = cleanType.substring(0, cleanType.indexOf(":"));
        String path = cleanType.substring(cleanType.indexOf(":") + 1);

        String formattedNamespace = capitalizeWords(namespace);
        String formattedPath = capitalizeWords(path.replace("_", " "));

        return formattedNamespace + ": " + formattedPath;
    }

    private static String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String[] words = input.split(" ");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(" ");
            }
            String word = words[i];
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
            }
        }

        return result.toString();
    }

    private record RecipeEntry(ResourceLocation id, Component displayName, String recipeType) {}

    private class RecipeList extends ObjectSelectionList<RecipeList.Entry> {

        RecipeList(Minecraft minecraft, int width, int height, int y, int bottom, int itemHeight,
                   Map<String, List<RecipeEntry>> recipesByType, List<String> sortedTypes) {
            super(minecraft, width, height, y, bottom, itemHeight);
            this.setRenderBackground(false);
            this.setRenderTopAndBottom(false);
            this.setRenderSelection(false);

            for (int i = 0; i < sortedTypes.size(); i++) {
                String typeName = sortedTypes.get(i);
                this.addEntry(new Entry(typeName));

                List<RecipeEntry> recipes = recipesByType.get(typeName);
                for (RecipeEntry recipe : recipes) {
                    this.addEntry(new Entry(recipe));
                }

                if (i < sortedTypes.size() - 1) {
                    this.addEntry(new Entry());
                }
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

        class Entry extends ObjectSelectionList.Entry<Entry> {
            private final RecipeEntry recipeEntry;
            private final String headerText;
            private final boolean isSpacer;
            private CompactCheckbox checkbox;

            Entry(RecipeEntry recipeEntry) {
                this.recipeEntry = recipeEntry;
                this.headerText = null;
                this.isSpacer = false;
                this.checkbox = new CompactCheckbox(
                    0,
                    0,
                    LedgerScreen.CONTENT_WIDTH - 10,
                    recipeEntry.displayName,
                    selectedRecipes.contains(recipeEntry.id)
                ) {
                    @Override
                    public void onPress() {
                        super.onPress();
                        onCheckboxChanged(this.selected());
                    }
                };
            }

            Entry(String headerText) {
                this.recipeEntry = null;
                this.headerText = headerText;
                this.isSpacer = false;
                this.checkbox = null;
            }

            private Entry() {
                this.recipeEntry = null;
                this.headerText = null;
                this.isSpacer = true;
                this.checkbox = null;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
                if (isSpacer) {
                    return;
                }
                if (headerText != null) {
                    guiGraphics.drawString(font, headerText, left, top, 0x3F3F3F, false);
                } else if (checkbox != null) {
                    this.checkbox.setX(left);
                    this.checkbox.setY(top);
                    this.checkbox.render(guiGraphics, mouseX, mouseY, partialTick);
                }
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (isSpacer) {
                    return false;
                }
                if (checkbox != null) {
                    return this.checkbox.mouseClicked(mouseX, mouseY, button);
                }
                return false;
            }

            @Override
            public Component getNarration() {
                if (isSpacer) {
                    return Component.empty();
                }
                if (headerText != null) {
                    return Component.literal(headerText);
                }
                return recipeEntry.displayName;
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
