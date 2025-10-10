package org.sosly.villagetale.gui.pages;

import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.sosly.villagetale.client.ZoneCreationManager;
import org.sosly.villagetale.gui.LedgerScreen;
import org.sosly.villagetale.gui.components.LedgerIconButton;

public class AddZonePage extends AbstractLedgerPage {
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 4;
    private static final int BUTTONS_Y_START = 40;

    public AddZonePage(LedgerScreen screen, UUID villageId) {
        super(screen, villageId);
    }

    @Override
    public void attach(int uStart, int vStart) {
        super.attach(uStart, vStart);

        int buttonX = uStart + (LedgerScreen.CONTENT_WIDTH - BUTTON_WIDTH) / 2;
        int currentY = vStart + BUTTONS_Y_START;

        addRenderableWidget(Button.builder(
                Component.translatable("villagetale.gui.add_zone.box"),
                b -> createBoxZone()
        ).bounds(buttonX, currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        currentY += BUTTON_HEIGHT + BUTTON_SPACING;

        addRenderableWidget(Button.builder(
                Component.translatable("villagetale.gui.add_zone.cylinder"),
                b -> createCylinderZone()
        ).bounds(buttonX, currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        currentY += BUTTON_HEIGHT + BUTTON_SPACING;

        addRenderableWidget(Button.builder(
                Component.translatable("villagetale.gui.add_zone.point"),
                b -> createPointZone()
        ).bounds(buttonX, currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        currentY += BUTTON_HEIGHT + BUTTON_SPACING;

        addRenderableWidget(Button.builder(
            Component.translatable("villagetale.gui.add_zone.route"),
            b -> createRouteZone()
        ).bounds(buttonX, currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        addRenderableWidget(LedgerIconButton.Back(
            uStart,
            vStart + LedgerScreen.CONTENT_HEIGHT - LedgerIconButton.BACK.height(),
            b -> screen.setRightPage(new ZoneListPage(screen, villageId)),
            Component.translatable("villagetale.gui.back")
        ));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.drawString(font, Component.translatable("villagetale.gui.add_zone.title"), uStart, vStart + 16, 0, false);
    }

    private void createBoxZone() {
        ZoneCreationManager.getInstance().startBoxCreation(villageId);
        screen.close();
    }

    private void createCylinderZone() {
        ZoneCreationManager.getInstance().startCylinderCreation(villageId);
        screen.close();
    }

    private void createPointZone() {
        ZoneCreationManager.getInstance().startPointCreation(villageId);
        screen.close();
    }

    private void createRouteZone() {
        ZoneCreationManager.getInstance().startRouteCreation(villageId);
        screen.close();
    }
}
