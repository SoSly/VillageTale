package org.sosly.villagetale.client.gui;

import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.sosly.villagetale.client.ZoneCreationManager;
import org.sosly.villagetale.client.gui.components.LedgerBackButton;

@OnlyIn(Dist.CLIENT)
public class AddZoneScreen extends AbstractLedgerScreen {
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 4;
    private static final int BUTTONS_Y_START = 40;

    private final UUID villageId;
    private final AbstractLedgerScreen previousScreen;
    private LedgerBackButton backButton;

    public AddZoneScreen(UUID villageId, AbstractLedgerScreen previousScreen) {
        super(Component.translatable("villagetale.gui.add_zone.title"));
        this.villageId = villageId;
        this.previousScreen = previousScreen;
    }

    @Override
    protected void init() {
        super.init();

        int leftPos = getLeftPos();
        int topPos = getTopPos();

        int buttonX = leftPos + (GUI_WIDTH - BUTTON_WIDTH) / 2;
        int currentY = topPos + BUTTONS_Y_START;

        this.addRenderableWidget(Button.builder(
            Component.translatable("villagetale.gui.add_zone.box"),
            button -> createBoxZone()
        ).bounds(buttonX, currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        currentY += BUTTON_HEIGHT + BUTTON_SPACING;

        this.addRenderableWidget(Button.builder(
            Component.translatable("villagetale.gui.add_zone.cylinder"),
            button -> createCylinderZone()
        ).bounds(buttonX, currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        currentY += BUTTON_HEIGHT + BUTTON_SPACING;

        this.addRenderableWidget(Button.builder(
            Component.translatable("villagetale.gui.add_zone.point"),
            button -> createPointZone()
        ).bounds(buttonX, currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        currentY += BUTTON_HEIGHT + BUTTON_SPACING;

        this.addRenderableWidget(Button.builder(
            Component.translatable("villagetale.gui.add_zone.route"),
            button -> createRouteZone()
        ).bounds(buttonX, currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.backButton = this.addRenderableWidget(new LedgerBackButton(
            leftPos + 62,
            topPos + 153,
            button -> returnToPreviousScreen()
        ));
    }

    @Override
    protected void renderLedgerContent(GuiGraphics guiGraphics, int leftPos, int topPos, int mouseX, int mouseY, float partialTick) {
        guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.add_zone.title"), leftPos + CONTENT_LEFT_MARGIN, topPos + 16, 0, false);
    }

    private void createBoxZone() {
        ZoneCreationManager.getInstance().startBoxCreation(villageId);

        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.MASTER, 0.5f, 1.0f);
            this.minecraft.setScreen(null);
        }
    }

    private void createCylinderZone() {
        ZoneCreationManager.getInstance().startCylinderCreation(villageId);

        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.MASTER, 0.5f, 1.0f);
            this.minecraft.setScreen(null);
        }
    }

    private void createPointZone() {
        ZoneCreationManager.getInstance().startPointCreation(villageId);

        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.MASTER, 0.5f, 1.0f);
            this.minecraft.setScreen(null);
        }
    }

    private void createRouteZone() {
    }

    private void returnToPreviousScreen() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(previousScreen);
        }
    }
}
