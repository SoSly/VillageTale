package org.sosly.villagetale.api.gui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Defines a single page within the Ledger GUI system.
 * <p>
 * Pages are individual screens that can be displayed in the Ledger, such as village info,
 * zone management, or villager assignment. Each page is responsible for managing its own
 * lifecycle, rendering, and tick updates within the context of the parent LedgerScreen.
 * </p>
 */
public interface ILedgerPage {
    /**
     * Attaches the page to the parent screen and initializes its widgets.
     * <p>
     * Called when the page is added to the LedgerScreen. Implementations should create and
     * register all widgets, buttons, and other UI components with the parent screen.
     * </p>
     *
     * @param uStart the x-coordinate of the left edge of the content area
     * @param vStart the y-coordinate of the top edge of the content area
     */
    void attach(int uStart, int vStart);

    /**
     * Detaches the page from the parent screen and cleans up its widgets.
     * <p>
     * Called when the page is removed from the LedgerScreen. Implementations must remove
     * all widgets and components that were added during attach() to prevent memory leaks
     * and ghost event listeners.
     * </p>
     */
    void detach();

    /**
     * Renders the page content at the specified position.
     * <p>
     * Called every frame to draw the page. Implementations should render all visual elements
     * including backgrounds, text, widgets, and tooltips.
     * </p>
     *
     * @param guiGraphics the graphics context for rendering
     * @param mouseX the current mouse x-coordinate
     * @param mouseY the current mouse y-coordinate
     * @param partialTick the partial tick time for smooth animations
     */
    void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);

    /**
     * Updates the page state each game tick.
     * <p>
     * Called once per tick (20 times per second) for time-based updates. Default implementation
     * does nothing. Override to implement animations, timers, or other tick-based logic.
     * </p>
     */
    default void tick() {}
}
