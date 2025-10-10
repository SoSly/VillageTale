package org.sosly.villagetale.client.gui.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.sosly.villagetale.client.gui.AbstractLedgerScreen;

@OnlyIn(Dist.CLIENT)
public class LedgerIconButton extends Button {
    private static final ButtonSpec ARROW_LEFT = new ButtonSpec(0, 282, 6, 11, 11);
    private static final ButtonSpec ARROW_RIGHT = new ButtonSpec(22, 282, 6, 11, 11);
    private static final ButtonSpec BACK = new ButtonSpec(0, 247, 14, 10, 15);
    private static final ButtonSpec CANCEL = new ButtonSpec(0, 235, 9, 9, 10);
    private static final ButtonSpec COMMIT = new ButtonSpec(0, 225, 9, 9, 10);
    private static final ButtonSpec DELETE = new ButtonSpec(0, 271, 7, 10, 9);
    private static final ButtonSpec EDIT = new ButtonSpec(0, 260, 10, 10, 10);
    private static final ButtonSpec NEW = new ButtonSpec(0, 215, 9, 9, 10);
    private static final ButtonSpec PAGE_NEXT = new ButtonSpec(0, 181, 18, 10, 19);
    private static final ButtonSpec PAGE_PREV = new ButtonSpec(0, 200, 18, 10, 19);

    private final int iconU;
    private final int iconV;
    private final int iconUWidth;
    private final int iconUHeight;
    private final int hoverShift;

    private LedgerIconButton(int x, int y, int iconU, int iconV, int iconUWidth, int iconUHeight, int hoverShift, OnPress onPress, Component message) {
        super(x, y, iconUWidth, iconUHeight, message, onPress, DEFAULT_NARRATION);
        this.iconU = iconU;
        this.iconV = iconV;
        this.iconUWidth = iconUWidth;
        this.iconUHeight = iconUHeight;
        this.hoverShift = hoverShift;
    }

    public static LedgerIconButton ArrowLeft(int x, int y, OnPress onPress, Component message) {
        return new LedgerIconButton(x, y, ARROW_LEFT.u, ARROW_LEFT.v, ARROW_LEFT.width, ARROW_LEFT.height, ARROW_LEFT.hoverShift, onPress, message);
    }

    public static LedgerIconButton ArrowRight(int x, int y, OnPress onPress, Component message) {
        return new LedgerIconButton(x, y, ARROW_RIGHT.u, ARROW_RIGHT.v, ARROW_RIGHT.width, ARROW_RIGHT.height, ARROW_RIGHT.hoverShift, onPress, message);
    }

    public static LedgerIconButton Back(int x, int y, OnPress onPress, Component message) {
        return new LedgerIconButton(x, y, BACK.u, BACK.v, BACK.width, BACK.height, BACK.hoverShift, onPress, message);
    }

    public static LedgerIconButton Commit(int x, int y, OnPress onPress, Component message) {
        return new LedgerIconButton(x, y, COMMIT.u, COMMIT.v, COMMIT.width, COMMIT.height, COMMIT.hoverShift, onPress, message);
    }

    public static LedgerIconButton Cancel(int x, int y, OnPress onPress, Component message) {
        return new LedgerIconButton(x, y, CANCEL.u, CANCEL.v, CANCEL.width, CANCEL.height, CANCEL.hoverShift, onPress, message);
    }

    public static LedgerIconButton Delete(int x, int y, OnPress onPress, Component message) {
        return new LedgerIconButton(x, y, DELETE.u, DELETE.v, DELETE.width, DELETE.height, DELETE.hoverShift, onPress, message);
    }

    public static LedgerIconButton Edit(int x, int y, OnPress onPress, Component message) {
        return new LedgerIconButton(x, y, EDIT.u, EDIT.v, EDIT.width, EDIT.height, EDIT.hoverShift, onPress, message);
    }

    public static LedgerIconButton New(int x, int y, OnPress onPress, Component message) {
        return new LedgerIconButton(x, y, NEW.u, NEW.v, NEW.width, NEW.height, NEW.hoverShift, onPress, message);
    }

    public static LedgerIconButton PageNext(int x, int y, OnPress onPress, Component message) {
        return new LedgerIconButton(x, y, PAGE_NEXT.u, PAGE_NEXT.v, PAGE_NEXT.width, PAGE_NEXT.height, PAGE_NEXT.hoverShift, onPress, message);
    }

    public static LedgerIconButton PagePrev(int x, int y, OnPress onPress, Component message) {
        return new LedgerIconButton(x, y, PAGE_PREV.u, PAGE_PREV.v, PAGE_PREV.width, PAGE_PREV.height, PAGE_PREV.hoverShift, onPress, message);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.blit(AbstractLedgerScreen.LEDGER_TEXTURE, this.getX(), this.getY(),
                this.isHovered() ? iconU + hoverShift : iconU,
                iconV, iconUWidth, iconUHeight, AbstractLedgerScreen.TEXTURE_WIDTH, AbstractLedgerScreen.TEXTURE_HEIGHT);
    }

    private record ButtonSpec(int u, int v, int width, int height, int hoverShift) {}
}
