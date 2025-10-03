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
import java.lang.reflect.Field;
import java.util.function.BiFunction;

@OnlyIn(Dist.CLIENT)
public class NoShadowEditBox extends EditBox {
    private static final Field FONT_FIELD;
    private static final Field FRAME_FIELD;
    private static final Field BORDERED_FIELD;
    private static final Field IS_EDITABLE_FIELD;
    private static final Field DISPLAY_POS_FIELD;
    private static final Field HIGHLIGHT_POS_FIELD;
    private static final Field TEXT_COLOR_FIELD;
    private static final Field TEXT_COLOR_UNEDITABLE_FIELD;
    private static final Field SUGGESTION_FIELD;
    private static final Field HINT_FIELD;
    private static final Field FORMATTER_FIELD;

    static {
        try {
            FONT_FIELD = EditBox.class.getDeclaredField("font");
            FONT_FIELD.setAccessible(true);

            FRAME_FIELD = EditBox.class.getDeclaredField("frame");
            FRAME_FIELD.setAccessible(true);

            BORDERED_FIELD = EditBox.class.getDeclaredField("bordered");
            BORDERED_FIELD.setAccessible(true);

            IS_EDITABLE_FIELD = EditBox.class.getDeclaredField("isEditable");
            IS_EDITABLE_FIELD.setAccessible(true);

            DISPLAY_POS_FIELD = EditBox.class.getDeclaredField("displayPos");
            DISPLAY_POS_FIELD.setAccessible(true);

            HIGHLIGHT_POS_FIELD = EditBox.class.getDeclaredField("highlightPos");
            HIGHLIGHT_POS_FIELD.setAccessible(true);

            TEXT_COLOR_FIELD = EditBox.class.getDeclaredField("textColor");
            TEXT_COLOR_FIELD.setAccessible(true);

            TEXT_COLOR_UNEDITABLE_FIELD = EditBox.class.getDeclaredField("textColorUneditable");
            TEXT_COLOR_UNEDITABLE_FIELD.setAccessible(true);

            SUGGESTION_FIELD = EditBox.class.getDeclaredField("suggestion");
            SUGGESTION_FIELD.setAccessible(true);

            HINT_FIELD = EditBox.class.getDeclaredField("hint");
            HINT_FIELD.setAccessible(true);

            FORMATTER_FIELD = EditBox.class.getDeclaredField("formatter");
            FORMATTER_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to initialize NoShadowEditBox reflection", e);
        }
    }

    public NoShadowEditBox(Font pFont, int pX, int pY, int pWidth, int pHeight, Component pMessage) {
        super(pFont, pX, pY, pWidth, pHeight, pMessage);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.isVisible()) {
            return;
        }

        try {
            Font font = (Font) FONT_FIELD.get(this);
            String value = this.getValue();
            int frame = FRAME_FIELD.getInt(this);
            boolean bordered = BORDERED_FIELD.getBoolean(this);
            boolean isEditable = IS_EDITABLE_FIELD.getBoolean(this);
            int displayPos = DISPLAY_POS_FIELD.getInt(this);
            int cursorPos = this.getCursorPosition();
            int highlightPos = HIGHLIGHT_POS_FIELD.getInt(this);
            int textColor = TEXT_COLOR_FIELD.getInt(this);
            int textColorUneditable = TEXT_COLOR_UNEDITABLE_FIELD.getInt(this);
            String suggestion = (String) SUGGESTION_FIELD.get(this);
            Component hint = (Component) HINT_FIELD.get(this);
            @SuppressWarnings("unchecked")
            BiFunction<String, Integer, FormattedCharSequence> formatter =
                (BiFunction<String, Integer, FormattedCharSequence>) FORMATTER_FIELD.get(this);

            if (bordered) {
                int borderColor = this.isFocused() ? -1 : -6250336;
                graphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.width + 1, this.getY() + this.height + 1, borderColor);
                graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, -16777216);
            }

            int color = isEditable ? textColor : textColorUneditable;
            int cursorOffset = cursorPos - displayPos;
            int highlightOffset = highlightPos - displayPos;
            String visibleText = font.plainSubstrByWidth(value.substring(displayPos), this.getInnerWidth());
            boolean cursorInBounds = cursorOffset >= 0 && cursorOffset <= visibleText.length();
            boolean showCursor = this.isFocused() && frame / 6 % 2 == 0 && cursorInBounds;

            int textX = bordered ? this.getX() + 4 : this.getX();
            int textY = bordered ? this.getY() + (this.height - 8) / 2 : this.getY();
            int textEndX = textX;

            int clampedHighlight = Math.min(highlightOffset, visibleText.length());

            if (!visibleText.isEmpty()) {
                String beforeCursor = cursorInBounds ? visibleText.substring(0, cursorOffset) : visibleText;
                textEndX = graphics.drawString(font, formatter.apply(beforeCursor, displayPos), textX, textY, color, false);
            }

            boolean cursorAtEnd = cursorPos < value.length() || value.length() >= this.getValue().length();
            int cursorX = textEndX;

            if (!cursorInBounds) {
                cursorX = cursorOffset > 0 ? textX + this.width : textX;
            } else if (cursorAtEnd) {
                cursorX = textEndX - 1;
                --textEndX;
            }

            if (!visibleText.isEmpty() && cursorInBounds && cursorOffset < visibleText.length()) {
                graphics.drawString(font, formatter.apply(visibleText.substring(cursorOffset), cursorPos), textEndX, textY, color, false);
            }

            if (hint != null && visibleText.isEmpty() && !this.isFocused()) {
                graphics.drawString(font, hint, textEndX, textY, color, false);
            }

            if (!cursorAtEnd && suggestion != null) {
                graphics.drawString(font, suggestion, cursorX - 1, textY, -8355712, false);
            }

            if (showCursor) {
                if (cursorAtEnd) {
                    graphics.fill(RenderType.guiOverlay(), cursorX, textY - 1, cursorX + 1, textY + 1 + 9, -3092272);
                } else {
                    graphics.drawString(font, "_", cursorX, textY, color, false);
                }
            }

            if (clampedHighlight != cursorOffset) {
                int highlightEndX = textX + font.width(visibleText.substring(0, clampedHighlight));
                this.renderHighlight(graphics, cursorX, textY - 1, highlightEndX - 1, textY + 1 + 9);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to render NoShadowEditBox", e);
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
