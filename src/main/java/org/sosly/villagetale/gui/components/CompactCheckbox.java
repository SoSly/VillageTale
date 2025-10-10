package org.sosly.villagetale.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CompactCheckbox extends AbstractButton {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/checkbox.png");
    private static final int CHECKBOX_SIZE = 8;
    private static final int TEXT_COLOR = 0x3F3F3F;
    private boolean selected;

    public CompactCheckbox(int x, int y, int width, Component message, boolean selected) {
        super(x, y, width, CHECKBOX_SIZE, message);
        this.selected = selected;
    }

    @Override
    public void onPress() {
        this.selected = !this.selected;
    }

    public boolean selected() {
        return this.selected;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narration) {
        narration.add(NarratedElementType.TITLE, this.createNarrationMessage());
        if (this.active) {
            if (this.isFocused()) {
                narration.add(NarratedElementType.USAGE, Component.translatable("narration.checkbox.usage.focused"));
            } else {
                narration.add(NarratedElementType.USAGE, Component.translatable("narration.checkbox.usage.hovered"));
            }
        }
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        RenderSystem.enableDepthTest();
        Font font = minecraft.font;

        guiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(0.4F, 0.4F, 1.0F);

        float scaledX = this.getX() * 2.5F;
        float scaledY = this.getY() * 2.5F;

        guiGraphics.blit(
            TEXTURE,
            (int) scaledX,
            (int) scaledY,
            this.isFocused() ? 20.0F : 0.0F,
            this.selected ? 20.0F : 0.0F,
            20,
            20,
            64,
            64
        );

        guiGraphics.pose().popPose();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        int textX = this.getX() + CHECKBOX_SIZE + 4;
        int textY = this.getY() + (CHECKBOX_SIZE - 8) / 2;
        guiGraphics.drawString(font, this.getMessage(), textX, textY, TEXT_COLOR | Mth.ceil(this.alpha * 255.0F) << 24, false);
    }
}
