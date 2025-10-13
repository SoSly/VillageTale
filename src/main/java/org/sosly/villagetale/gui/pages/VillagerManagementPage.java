package org.sosly.villagetale.gui.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.sosly.villagetale.api.IProfession;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.client.ClientDataManager;
import org.sosly.villagetale.client.VillageDataManager;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.event.VillagerInteractionHandler;
import org.sosly.villagetale.gui.LedgerScreen;
import org.sosly.villagetale.gui.components.LedgerIconButton;
import org.sosly.villagetale.network.packets.serverbound.UpdateVillagerAssignment;
import org.sosly.villagetale.profession.ProfessionRegistry;
import org.sosly.villagetale.zone.type.Home;

public class VillagerManagementPage extends AbstractLedgerPage {
    private final int villagerEntityId;
    private final UUID initialHomeZoneId;
    private final UUID initialWorkZoneId;

    private List<ResourceLocation> professions;
    private int currentProfessionIndex;
    private LedgerIconButton professionLeftButton;
    private LedgerIconButton professionRightButton;

    private List<IVillageZone> homeZones;
    private int currentHomeZoneIndex;
    private LedgerIconButton homeZoneLeftButton;
    private LedgerIconButton homeZoneRightButton;

    private List<IVillageZone> workZones;
    private int currentWorkZoneIndex;
    private LedgerIconButton workZoneLeftButton;
    private LedgerIconButton workZoneRightButton;

    private LedgerIconButton recipeEditButton;

    private int selectionStartWidth = 3;
    private int maxLineLength = 19;

    public VillagerManagementPage(LedgerScreen screen, int villagerEntityId, UUID villageId, UUID homeZoneId, UUID workZoneId) {
        super(screen, villageId);
        this.villagerEntityId = villagerEntityId;
        this.initialHomeZoneId = homeZoneId;
        this.initialWorkZoneId = workZoneId;
        this.professions = new ArrayList<>();
        this.homeZones = new ArrayList<>();
        this.workZones = new ArrayList<>();
    }

    @Override
    public void attach(int uStart, int vStart) {
        super.attach(uStart, vStart);

        Villager villager = getVillager();
        if (villager == null) {
            return;
        }

        IVillageCapability village = VillageDataManager.getInstance().getVillageData(villageId);
        if (village == null) {
            return;
        }

        this.professions = new ArrayList<>(ProfessionRegistry.INSTANCE.getProfessionIDs());
        this.professions.sort((a, b) -> a.getPath().compareTo(b.getPath()));

        ResourceLocation currentProfession = ClientDataManager.getCachedProfession(villagerEntityId);
        if (currentProfession == null) {
            currentProfession = ProfessionRegistry.INSTANCE.getProfessionIDs().stream().findFirst().orElse(null);
        }
        this.currentProfessionIndex = currentProfession != null ? professions.indexOf(currentProfession) : 0;
        if (this.currentProfessionIndex < 0) {
            this.currentProfessionIndex = 0;
        }

        this.homeZones = village.getZones().stream()
            .filter(z -> z.getType() != null && z.getType().getID().equals(Home.ID))
            .sorted((a, b) -> a.getName().compareTo(b.getName()))
            .toList();

        this.currentHomeZoneIndex = initialHomeZoneId != null
            ? findZoneIndex(homeZones, initialHomeZoneId)
            : -1;

        updateWorkZones(villager);

        this.currentWorkZoneIndex = initialWorkZoneId != null
            ? findZoneIndex(workZones, initialWorkZoneId)
            : -1;

        initButtons();
    }

    @Override
    public void detach() {
        super.detach();
        VillagerInteractionHandler.releaseVillager(villagerEntityId);
        this.professions = null;
        this.homeZones = null;
        this.workZones = null;
        this.professionLeftButton = null;
        this.professionRightButton = null;
        this.homeZoneLeftButton = null;
        this.homeZoneRightButton = null;
        this.workZoneLeftButton = null;
        this.workZoneRightButton = null;
        this.recipeEditButton = null;
    }

    private void initButtons() {
        int currentY = vStart + 16;
        currentY += LINE_HEIGHT;
        currentY += LINE_HEIGHT;

        this.professionLeftButton = LedgerIconButton.arrowLeft(
            uStart,
            currentY - 3,
            button -> cycleProfessionPrevious(),
            Component.translatable("villagetale.gui.villager_management.profession")
        );
        addRenderableWidget(this.professionLeftButton);

        this.professionRightButton = LedgerIconButton.arrowRight(
            uStart + LedgerScreen.CONTENT_WIDTH - LedgerIconButton.ARROW_RIGHT.width(),
            currentY - 3,
            button -> cycleProfessionNext(),
            Component.translatable("villagetale.gui.villager_management.profession")
        );
        addRenderableWidget(this.professionRightButton);

        currentY += LINE_HEIGHT;
        currentY += LINE_HEIGHT;

        this.workZoneLeftButton = LedgerIconButton.arrowLeft(
            uStart,
            currentY - 3,
            button -> cycleWorkZonePrevious(),
            Component.translatable("villagetale.gui.villager_management.work_zone")
        );
        this.workZoneLeftButton.visible = !workZones.isEmpty();
        addRenderableWidget(this.workZoneLeftButton);

        this.workZoneRightButton = LedgerIconButton.arrowRight(
            uStart + LedgerScreen.CONTENT_WIDTH - LedgerIconButton.ARROW_RIGHT.width(),
            currentY - 3,
            button -> cycleWorkZoneNext(),
            Component.translatable("villagetale.gui.villager_management.work_zone")
        );
        this.workZoneRightButton.visible = !workZones.isEmpty();
        addRenderableWidget(this.workZoneRightButton);

        currentY += LINE_HEIGHT;
        currentY += LINE_HEIGHT;

        this.homeZoneLeftButton = LedgerIconButton.arrowLeft(
            uStart,
            currentY - 3,
            button -> cycleHomeZonePrevious(),
            Component.translatable("villagetale.gui.villager_management.home_zone")
        );
        this.homeZoneLeftButton.visible = !homeZones.isEmpty();
        addRenderableWidget(this.homeZoneLeftButton);

        this.homeZoneRightButton = LedgerIconButton.arrowRight(
            uStart + LedgerScreen.CONTENT_WIDTH - LedgerIconButton.ARROW_RIGHT.width(),
            currentY - 3,
            button -> cycleHomeZoneNext(),
            Component.translatable("villagetale.gui.villager_management.home_zone")
        );
        this.homeZoneRightButton.visible = !homeZones.isEmpty();
        addRenderableWidget(this.homeZoneRightButton);

        currentY += LINE_HEIGHT;
        currentY += LINE_HEIGHT;

        this.recipeEditButton = LedgerIconButton.edit(
            uStart + LedgerScreen.CONTENT_WIDTH - LedgerIconButton.EDIT.width(),
            currentY - 1,
            button -> openRecipeConfig(),
            Component.literal("Edit Recipes")
        );
        updateRecipeButtonVisibility();
        addRenderableWidget(this.recipeEditButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Villager villager = getVillager();
        if (villager == null) {
            guiGraphics.drawString(font, Component.literal("Villager not found"), uStart, vStart + 16, 0x3F3F3F, false);
            return;
        }

        int currentY = vStart + 16;

        guiGraphics.drawString(font, villager.getDisplayName(), uStart, currentY, 0, false);
        currentY += LINE_HEIGHT;

        guiGraphics.drawString(font, Component.translatable("villagetale.gui.villager_management.profession"), uStart, currentY, 0, false);
        currentY += LINE_HEIGHT;

        if (!professions.isEmpty() && currentProfessionIndex >= 0 && currentProfessionIndex < professions.size()) {
            ResourceLocation profession = professions.get(currentProfessionIndex);
            String professionName = profession.getPath();
            int professionNameX = uStart + LedgerIconButton.ARROW_LEFT.width() + selectionStartWidth;
            guiGraphics.drawString(font, Component.literal(professionName), professionNameX, currentY, 0, false);
        }
        currentY += LINE_HEIGHT;

        guiGraphics.drawString(font, Component.translatable("villagetale.gui.villager_management.work_zone"), uStart, currentY, 0, false);
        currentY += LINE_HEIGHT;

        if (workZones.isEmpty() || currentWorkZoneIndex < 0) {
            int noneX = uStart + LedgerIconButton.ARROW_LEFT.width() + selectionStartWidth;
            guiGraphics.drawString(font, Component.literal("None"), noneX, currentY, 0x3F3F3F, false);
        } else if (currentWorkZoneIndex < workZones.size()) {
            IVillageZone workZone = workZones.get(currentWorkZoneIndex);
            String zoneName = workZone.getName();
            if (zoneName.length() > maxLineLength) {
                zoneName = zoneName.substring(0, maxLineLength - 3) + "...";
            }
            int zoneNameX = uStart + LedgerIconButton.ARROW_LEFT.width() + selectionStartWidth;
            guiGraphics.drawString(font, Component.literal(zoneName), zoneNameX, currentY, 0, false);
        }
        currentY += LINE_HEIGHT;

        guiGraphics.drawString(font, Component.translatable("villagetale.gui.villager_management.home_zone"), uStart, currentY, 0, false);
        currentY += LINE_HEIGHT;

        if (homeZones.isEmpty() || currentHomeZoneIndex < 0) {
            int noneX = uStart + LedgerIconButton.ARROW_LEFT.width() + selectionStartWidth;
            guiGraphics.drawString(font, Component.literal("None"), noneX, currentY, 0x3F3F3F, false);
        } else if (currentHomeZoneIndex < homeZones.size()) {
            IVillageZone homeZone = homeZones.get(currentHomeZoneIndex);
            String zoneName = homeZone.getName();
            if (zoneName.length() > maxLineLength) {
                zoneName = zoneName.substring(0, maxLineLength - 3) + "...";
            }
            int zoneNameX = uStart + LedgerIconButton.ARROW_LEFT.width() + selectionStartWidth;
            guiGraphics.drawString(font, Component.literal(zoneName), zoneNameX, currentY, 0, false);
        }
        currentY += LINE_HEIGHT;

        IProfession profession = currentProfessionIndex >= 0 && currentProfessionIndex < professions.size()
            ? ProfessionRegistry.INSTANCE.getProfession(professions.get(currentProfessionIndex)).orElse(null)
            : null;

        if (profession != null && !profession.getLearnableItems().isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("villagetale.gui.villager_management.recipes"), uStart, currentY, 0, false);
            currentY += LINE_HEIGHT;

            Set<ResourceLocation> recipes = ClientDataManager.getCachedRecipes(villagerEntityId);
            int recipeCount = recipes != null ? recipes.size() : 0;
            String recipeText = "Recipes: " + recipeCount;
            guiGraphics.drawString(font, Component.literal(recipeText), uStart, currentY, 0, false);
        }
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

    private void updateWorkZones(Villager villager) {
        IVillageCapability village = VillageDataManager.getInstance().getVillageData(villageId);
        if (village == null) {
            this.workZones = new ArrayList<>();
            return;
        }

        if (professions.isEmpty() || currentProfessionIndex < 0 || currentProfessionIndex >= professions.size()) {
            this.workZones = new ArrayList<>();
            return;
        }

        ResourceLocation professionId = professions.get(currentProfessionIndex);
        IProfession profession = ProfessionRegistry.INSTANCE.getProfession(professionId).orElse(null);
        if (profession == null) {
            this.workZones = new ArrayList<>();
            return;
        }

        this.workZones = village.getZones().stream()
            .filter(z -> profession.isValidWorkZone(z))
            .sorted((a, b) -> a.getName().compareTo(b.getName()))
            .toList();
    }

    private int findZoneIndex(List<IVillageZone> zones, UUID zoneId) {
        for (int i = 0; i < zones.size(); i++) {
            if (zones.get(i).getUUID().equals(zoneId)) {
                return i;
            }
        }
        return -1;
    }

    private void cycleProfessionPrevious() {
        if (professions.isEmpty()) {
            return;
        }
        currentProfessionIndex--;
        if (currentProfessionIndex < 0) {
            currentProfessionIndex = professions.size() - 1;
        }
        onProfessionChanged();
    }

    private void cycleProfessionNext() {
        if (professions.isEmpty()) {
            return;
        }
        currentProfessionIndex++;
        if (currentProfessionIndex >= professions.size()) {
            currentProfessionIndex = 0;
        }
        onProfessionChanged();
    }

    private void onProfessionChanged() {
        if (professions.isEmpty() || currentProfessionIndex < 0 || currentProfessionIndex >= professions.size()) {
            return;
        }

        Villager villager = getVillager();
        if (villager == null) {
            return;
        }

        ResourceLocation newProfession = professions.get(currentProfessionIndex);
        UUID homeZoneId = currentHomeZoneIndex >= 0 && currentHomeZoneIndex < homeZones.size()
            ? homeZones.get(currentHomeZoneIndex).getUUID()
            : null;

        updateWorkZones(villager);
        currentWorkZoneIndex = -1;

        if (this.workZoneLeftButton != null) {
            this.workZoneLeftButton.visible = !workZones.isEmpty();
        }
        if (this.workZoneRightButton != null) {
            this.workZoneRightButton.visible = !workZones.isEmpty();
        }

        updateRecipeButtonVisibility();

        UpdateVillagerAssignment.send(villagerEntityId, newProfession, homeZoneId, null, true);
    }

    private void updateRecipeButtonVisibility() {
        if (this.recipeEditButton == null) {
            return;
        }

        IProfession profession = currentProfessionIndex >= 0 && currentProfessionIndex < professions.size()
            ? ProfessionRegistry.INSTANCE.getProfession(professions.get(currentProfessionIndex)).orElse(null)
            : null;

        this.recipeEditButton.visible = profession != null && !profession.getLearnableItems().isEmpty();
    }

    private void cycleHomeZonePrevious() {
        if (homeZones.isEmpty()) {
            return;
        }
        currentHomeZoneIndex--;
        if (currentHomeZoneIndex < 0) {
            currentHomeZoneIndex = homeZones.size() - 1;
        }
        onHomeZoneChanged();
    }

    private void cycleHomeZoneNext() {
        if (homeZones.isEmpty()) {
            return;
        }
        currentHomeZoneIndex++;
        if (currentHomeZoneIndex >= homeZones.size()) {
            currentHomeZoneIndex = 0;
        }
        onHomeZoneChanged();
    }

    private void onHomeZoneChanged() {
        if (homeZones.isEmpty() || currentHomeZoneIndex < 0 || currentHomeZoneIndex >= homeZones.size()) {
            return;
        }

        ResourceLocation profession = currentProfessionIndex >= 0 && currentProfessionIndex < professions.size()
            ? professions.get(currentProfessionIndex)
            : null;
        UUID homeZoneId = homeZones.get(currentHomeZoneIndex).getUUID();
        UUID workZoneId = currentWorkZoneIndex >= 0 && currentWorkZoneIndex < workZones.size()
            ? workZones.get(currentWorkZoneIndex).getUUID()
            : null;

        UpdateVillagerAssignment.send(villagerEntityId, profession, homeZoneId, workZoneId, false);
    }

    private void cycleWorkZonePrevious() {
        if (workZones.isEmpty()) {
            return;
        }
        currentWorkZoneIndex--;
        if (currentWorkZoneIndex < 0) {
            currentWorkZoneIndex = workZones.size() - 1;
        }
        onWorkZoneChanged();
    }

    private void cycleWorkZoneNext() {
        if (workZones.isEmpty()) {
            return;
        }
        currentWorkZoneIndex++;
        if (currentWorkZoneIndex >= workZones.size()) {
            currentWorkZoneIndex = 0;
        }
        onWorkZoneChanged();
    }

    private void onWorkZoneChanged() {
        if (workZones.isEmpty() || currentWorkZoneIndex < 0 || currentWorkZoneIndex >= workZones.size()) {
            return;
        }

        ResourceLocation profession = currentProfessionIndex >= 0 && currentProfessionIndex < professions.size()
            ? professions.get(currentProfessionIndex)
            : null;
        UUID homeZoneId = currentHomeZoneIndex >= 0 && currentHomeZoneIndex < homeZones.size()
            ? homeZones.get(currentHomeZoneIndex).getUUID()
            : null;
        UUID workZoneId = workZones.get(currentWorkZoneIndex).getUUID();

        UpdateVillagerAssignment.send(villagerEntityId, profession, homeZoneId, workZoneId, false);
    }

    private void openRecipeConfig() {
        screen.setRightPage(new RecipeConfigurationPage(screen, villageId, villagerEntityId));
    }
}
