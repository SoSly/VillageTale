package org.sosly.villagetale.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.function.BiFunction;

@OnlyIn(Dist.CLIENT)
public class NoShadowEditBox extends EditBox {
    private final Font font;
    private String value = "";
    private int frame;
    private boolean bordered = true;
    private boolean isEditable = true;
    private int displayPos;
    private int cursorPos;
    private int highlightPos;
    private int textColor = 14737632;
    private int textColorUneditable = 7368816;
    @Nullable
    private String suggestion;
    @Nullable
    private Component hint;
    private BiFunction<String, Integer, FormattedCharSequence> formatter = (text, pos) ->
        FormattedCharSequence.forward(text, net.minecraft.network.chat.Style.EMPTY);

    public NoShadowEditBox(Font pFont, int pX, int pY, int pWidth, int pHeight, Component pMessage) {
        super(pFont, pX, pY, pWidth, pHeight, pMessage);
        this.font = pFont;
    }

    @Override
    public void tick() {
        super.tick();
        ++this.frame;
    }

    @Override
    public void setValue(String pText) {
        super.setValue(pText);
        this.value = super.getValue();
        this.cursorPos = super.getCursorPosition();
        this.highlightPos = this.cursorPos;
    }

    @Override
    public void setBordered(boolean pEnableBackgroundDrawing) {
        super.setBordered(pEnableBackgroundDrawing);
        this.bordered = pEnableBackgroundDrawing;
    }

    @Override
    public void setTextColor(int pColor) {
        super.setTextColor(pColor);
        this.textColor = pColor;
    }

    @Override
    public void setTextColorUneditable(int pColor) {
        super.setTextColorUneditable(pColor);
        this.textColorUneditable = pColor;
    }

    @Override
    public void setEditable(boolean pEnabled) {
        super.setEditable(pEnabled);
        this.isEditable = pEnabled;
    }

    @Override
    public void setSuggestion(@Nullable String pSuggestion) {
        super.setSuggestion(pSuggestion);
        this.suggestion = pSuggestion;
    }

    @Override
    public void setHint(Component pHint) {
        super.setHint(pHint);
        this.hint = pHint;
    }

    @Override
    public void setFormatter(BiFunction<String, Integer, FormattedCharSequence> pTextFormatter) {
        super.setFormatter(pTextFormatter);
        this.formatter = pTextFormatter;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.isVisible()) {
            return;
        }

        this.value = super.getValue();
        this.cursorPos = super.getCursorPosition();
        this.highlightPos = this.cursorPos;

        if (this.bordered) {
            int borderColor = this.isFocused() ? -1 : -6250336;
            graphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.width + 1, this.getY() + this.height + 1, borderColor);
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, -16777216);
        }

        int color = this.isEditable ? this.textColor : this.textColorUneditable;
        int cursorOffset = this.cursorPos - this.displayPos;
        int highlightOffset = this.highlightPos - this.displayPos;
        String visibleText = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), this.getInnerWidth());
        boolean cursorInBounds = cursorOffset >= 0 && cursorOffset <= visibleText.length();
        boolean showCursor = this.isFocused() && this.frame / 6 % 2 == 0 && cursorInBounds;

        int textX = this.bordered ? this.getX() + 4 : this.getX();
        int textY = this.bordered ? this.getY() + (this.height - 8) / 2 : this.getY();
        int textEndX = textX;

        int clampedHighlight = Math.min(highlightOffset, visibleText.length());

        if (!visibleText.isEmpty()) {
            String beforeCursor = cursorInBounds ? visibleText.substring(0, cursorOffset) : visibleText;
            textEndX = graphics.drawString(this.font, this.formatter.apply(beforeCursor, this.displayPos), textX, textY, color, false);
        }

        boolean cursorAtEnd = this.cursorPos < this.value.length() || this.value.length() >= super.getValue().length();
        int cursorX = textEndX;

        if (!cursorInBounds) {
            cursorX = cursorOffset > 0 ? textX + this.width : textX;
        } else if (cursorAtEnd) {
            cursorX = textEndX - 1;
            --textEndX;
        }

        if (!visibleText.isEmpty() && cursorInBounds && cursorOffset < visibleText.length()) {
            graphics.drawString(this.font, this.formatter.apply(visibleText.substring(cursorOffset), this.cursorPos), textEndX, textY, color, false);
        }

        if (this.hint != null && visibleText.isEmpty() && !this.isFocused()) {
            graphics.drawString(this.font, this.hint, textEndX, textY, color, false);
        }

        if (!cursorAtEnd && this.suggestion != null) {
            graphics.drawString(this.font, this.suggestion, cursorX - 1, textY, -8355712, false);
        }

        if (showCursor) {
            if (cursorAtEnd) {
                graphics.fill(RenderType.guiOverlay(), cursorX, textY - 1, cursorX + 1, textY + 1 + 9, -3092272);
            } else {
                graphics.drawString(this.font, "_", cursorX, textY, color, false);
            }
        }

        if (clampedHighlight != cursorOffset) {
            int highlightEndX = textX + this.font.width(visibleText.substring(0, clampedHighlight));
            this.renderHighlight(graphics, cursorX, textY - 1, highlightEndX - 1, textY + 1 + 9);
        }
    }

    private void renderHighlight(GuiGraphics graphics, int minX, int minY, int maxX, int maxY) {
        if (minX >= maxX) {
            int temp = minX;
            minX = maxX;
            maxX = temp;
        }

        if (minY >= maxY) {
            int temp = minY;
            minY = maxY;
            maxY = temp;
        }

        if (maxX > this.getX() + this.width) {
            maxX = this.getX() + this.width;
        }

        if (minX > this.getX() + this.width) {
            minX = this.getX() + this.width;
        }

        graphics.fill(RenderType.guiTextHighlight(), minX, minY, maxX, maxY, -16776961);
    }
}
