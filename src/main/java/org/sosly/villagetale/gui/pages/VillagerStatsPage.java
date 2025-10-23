package org.sosly.villagetale.gui.pages;

import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.sosly.villagetale.gui.LedgerScreen;

public class VillagerStatsPage extends AbstractLedgerPage {
    private static final ResourceLocation GUI_ICONS_LOCATION = new ResourceLocation("textures/gui/icons.png");

    private final int villagerEntityId;
    private final float health;
    private final int hunger;
    private final int physique;
    private final int endurance;
    private final int intellect;

    public VillagerStatsPage(LedgerScreen screen, int villagerEntityId, UUID villageId, float health, int hunger, int physique, int endurance, int intellect) {
        super(screen, villageId);
        this.villagerEntityId = villagerEntityId;
        this.health = health;
        this.hunger = hunger;
        this.physique = physique;
        this.endurance = endurance;
        this.intellect = intellect;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int currentY = vStart + 16;

        guiGraphics.drawString(font, Component.literal("Stats"), uStart, currentY, 0, false);
        currentY += LINE_HEIGHT;

        guiGraphics.drawString(font, Component.literal("Health:"), uStart, currentY, 0, false);
        currentY += LINE_HEIGHT;
        renderVillagerHearts(guiGraphics, uStart, currentY);

        currentY += LINE_HEIGHT;

        guiGraphics.drawString(font, Component.literal("Hunger:"), uStart, currentY, 0, false);
        currentY += LINE_HEIGHT;
        renderVillagerHunger(guiGraphics, uStart, currentY);

        currentY += LINE_HEIGHT * 2;

        guiGraphics.drawString(font, Component.literal("Physique: " + physique), uStart, currentY, 0, false);
        currentY += LINE_HEIGHT;

        guiGraphics.drawString(font, Component.literal("Intellect: " + intellect), uStart, currentY, 0, false);
        currentY += LINE_HEIGHT;

        guiGraphics.drawString(font, Component.literal("Endurance: " + endurance), uStart, currentY, 0, false);
    }

    private void renderVillagerHearts(GuiGraphics guiGraphics, int x, int y) {
        int healthValue = (int) Math.ceil(health);
        int maxHealth = 20;
        int hearts = maxHealth / 2;

        for (int i = 0; i < hearts; i++) {
            int heartX = x + i * 8;
            int heartIndex = i * 2 + 1;

            guiGraphics.blit(GUI_ICONS_LOCATION, heartX, y, 16, 0, 9, 9);

            if (heartIndex < healthValue) {
                guiGraphics.blit(GUI_ICONS_LOCATION, heartX, y, 52, 0, 9, 9);
            } else if (heartIndex == healthValue) {
                guiGraphics.blit(GUI_ICONS_LOCATION, heartX, y, 61, 0, 9, 9);
            }
        }
    }

    private void renderVillagerHunger(GuiGraphics guiGraphics, int x, int y) {
        for (int i = 0; i < 10; i++) {
            int drumstickX = x + i * 8;
            int hungerIndex = i * 2 + 1;

            guiGraphics.blit(GUI_ICONS_LOCATION, drumstickX, y, 16, 27, 9, 9);

            if (hungerIndex < hunger) {
                guiGraphics.blit(GUI_ICONS_LOCATION, drumstickX, y, 52, 27, 9, 9);
            } else if (hungerIndex == hunger) {
                guiGraphics.blit(GUI_ICONS_LOCATION, drumstickX, y, 61, 27, 9, 9);
            }
        }
    }
}
