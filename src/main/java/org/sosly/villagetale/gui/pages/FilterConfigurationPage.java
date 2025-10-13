package org.sosly.villagetale.gui.pages;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.client.VillageDataManager;
import org.sosly.villagetale.gui.LedgerScreen;
import org.sosly.villagetale.gui.components.CompactCheckbox;
import org.sosly.villagetale.gui.components.LedgerIconButton;
import org.sosly.villagetale.network.packets.serverbound.UpdateZoneFilters;
import org.sosly.villagetale.zone.type.AbstractZoneType;

public class FilterConfigurationPage extends AbstractLedgerPage {
    private static final int LIST_TOP = 36;
    private static final int LIST_BOTTOM = 145;
    private static final int CLEAR_BUTTON_SIZE = 10;

    public enum FilterType {
        ITEM,
        ENTITY
    }

    private final UUID zoneId;
    private final FilterType filterType;
    private final int zoneIndex;
    private FilterList filterList;
    private LedgerIconButton clearAllButton;
    private LedgerIconButton backButton;
    private Set<ResourceLocation> selectedFilters;

    public FilterConfigurationPage(LedgerScreen screen, UUID villageId, UUID zoneId, FilterType filterType, int zoneIndex) {
        super(screen, villageId);
        this.zoneId = zoneId;
        this.filterType = filterType;
        this.zoneIndex = zoneIndex;
        this.selectedFilters = new HashSet<>();
    }

    @Override
    public void attach(int uStart, int vStart) {
        super.attach(uStart, vStart);

        IVillageCapability village = VillageDataManager.getInstance().getVillageData(villageId);
        if (village == null) {
            return;
        }

        IVillageZone zone = village.getZones().stream()
            .filter(z -> z.getUUID().equals(zoneId))
            .findFirst()
            .orElse(null);

        if (zone == null) {
            return;
        }

        if (filterType == FilterType.ITEM) {
            zone.getFilter().forEach(stack -> selectedFilters.add(BuiltInRegistries.ITEM.getKey(stack.getItem())));
        } else {
            selectedFilters.addAll(zone.getEntityTypeFilter());
        }

        List<FilterEntry> entries = new ArrayList<>();
        if (filterType == FilterType.ITEM) {
            if (zone.getType() instanceof AbstractZoneType zoneType) {
                List<ResourceLocation> itemIds = zoneType.getItemFilter().getAllItemIds();
                for (ResourceLocation itemId : itemIds) {
                    Item item = BuiltInRegistries.ITEM.get(itemId);
                    if (item != null) {
                        String displayName = item.getName(item.getDefaultInstance()).getString();
                        entries.add(new FilterEntry(itemId, displayName));
                    }
                }
            }
        } else {
            if (zone.getType() instanceof AbstractZoneType zoneType) {
                Set<ResourceLocation> entityIds = zoneType.getEntityFilter();
                for (ResourceLocation entityId : entityIds) {
                    EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityId);
                    if (entityType != null) {
                        String displayName = entityType.getDescription().getString();
                        entries.add(new FilterEntry(entityId, displayName));
                    }
                }
            }
        }

        this.filterList = new FilterList(
            screen.getMinecraft(),
            LedgerScreen.CONTENT_WIDTH,
            LIST_BOTTOM - LIST_TOP,
            vStart + LIST_TOP,
            vStart + LIST_BOTTOM,
            12,
            entries
        );
        this.filterList.setLeftPos(uStart);
        addWidget(this.filterList);

        this.clearAllButton = LedgerIconButton.delete(
            uStart + LedgerScreen.CONTENT_WIDTH - CLEAR_BUTTON_SIZE,
            vStart + 16,
            button -> clearAllFilters(),
            Component.translatable("villagetale.gui.filter.clear_all")
        );
        addRenderableWidget(this.clearAllButton);

        this.backButton = LedgerIconButton.back(
            uStart + (LedgerScreen.CONTENT_WIDTH - 14) / 2,
            vStart + 153,
            button -> closeFilters(),
            Component.translatable("villagetale.gui.back")
        );
        addRenderableWidget(this.backButton);
    }

    @Override
    public void detach() {
        super.detach();
        this.filterList = null;
        this.clearAllButton = null;
        this.backButton = null;
        this.selectedFilters.clear();
    }

    private void closeFilters() {
        screen.setLeftPage(new ZoneInfoPage(screen, villageId, zoneIndex));
        screen.setRightPage(new ZoneVillagersPage(screen, villageId, zoneIndex));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        IVillageCapability village = VillageDataManager.getInstance().getVillageData(villageId);
        if (village == null) {
            guiGraphics.drawString(font, Component.translatable("villagetale.gui.error.village_not_found"), uStart, vStart + 28, 0x3F3F3F, false);
            return;
        }

        IVillageZone zone = village.getZones().stream()
            .filter(z -> z.getUUID().equals(zoneId))
            .findFirst()
            .orElse(null);

        if (zone == null) {
            guiGraphics.drawString(font, Component.translatable("villagetale.gui.error.zone_not_found"), uStart, vStart + 28, 0x3F3F3F, false);
            return;
        }

        Component title = Component.translatable(filterType == FilterType.ITEM
            ? "villagetale.gui.filter.configure_item_filters"
            : "villagetale.gui.filter.configure_entity_filters");
        guiGraphics.drawString(font, title, uStart, vStart + 16, 0, false);

        if (this.filterList != null) {
            this.filterList.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    private void clearAllFilters() {
        selectedFilters.clear();
        sendFilterUpdate();
        if (this.filterList != null) {
            this.filterList.updateCheckboxStates();
        }
    }

    private void sendFilterUpdate() {
        UpdateZoneFilters.FilterType type = filterType == FilterType.ITEM
            ? UpdateZoneFilters.FilterType.ITEM
            : UpdateZoneFilters.FilterType.ENTITY;
        UpdateZoneFilters.send(villageId, zoneId, type, new ArrayList<>(selectedFilters));
    }

    private record FilterEntry(ResourceLocation id, String displayName) {}

    private class FilterList extends ObjectSelectionList<FilterList.Entry> {
        private final List<FilterEntry> filterEntries;

        FilterList(Minecraft minecraft, int width, int height, int y, int bottom, int itemHeight, List<FilterEntry> entries) {
            super(minecraft, width, height, y, bottom, itemHeight);
            this.filterEntries = entries;
            this.setRenderBackground(false);
            this.setRenderTopAndBottom(false);
            this.setRenderSelection(false);
            for (FilterEntry entry : entries) {
                this.addEntry(new Entry(entry));
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

        public void updateCheckboxStates() {
            this.children().forEach(Entry::updateCheckboxState);
        }

        class Entry extends ObjectSelectionList.Entry<Entry> {
            private final FilterEntry filterEntry;
            private CompactCheckbox checkbox;

            Entry(FilterEntry filterEntry) {
                this.filterEntry = filterEntry;
                this.checkbox = new CompactCheckbox(
                    0,
                    0,
                    LedgerScreen.CONTENT_WIDTH - 10,
                    Component.literal(filterEntry.displayName),
                    selectedFilters.contains(filterEntry.id)
                ) {
                    @Override
                    public void onPress() {
                        super.onPress();
                        onCheckboxChanged(this.selected());
                    }
                };
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
                this.checkbox.setX(left);
                this.checkbox.setY(top);
                this.checkbox.render(guiGraphics, mouseX, mouseY, partialTick);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return this.checkbox.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public Component getNarration() {
                return Component.literal(filterEntry.displayName);
            }

            private void onCheckboxChanged(boolean checked) {
                if (checked) {
                    selectedFilters.add(filterEntry.id);
                } else {
                    selectedFilters.remove(filterEntry.id);
                }
                sendFilterUpdate();
            }

            public void updateCheckboxState() {
                boolean shouldBeSelected = selectedFilters.contains(filterEntry.id);
                if (this.checkbox.selected() != shouldBeSelected) {
                    this.checkbox = new CompactCheckbox(
                        this.checkbox.getX(),
                        this.checkbox.getY(),
                        LedgerScreen.CONTENT_WIDTH - 10,
                        Component.literal(filterEntry.displayName),
                        shouldBeSelected
                    ) {
                        @Override
                        public void onPress() {
                            super.onPress();
                            onCheckboxChanged(this.selected());
                        }
                    };
                }
            }
        }
    }
}
