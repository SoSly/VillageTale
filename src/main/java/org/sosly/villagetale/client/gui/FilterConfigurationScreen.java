package org.sosly.villagetale.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.client.VillageDataManager;
import org.sosly.villagetale.client.gui.components.CompactCheckbox;
import org.sosly.villagetale.client.gui.components.LedgerBackButton;
import org.sosly.villagetale.network.packets.serverbound.UpdateZoneFilters;
import org.sosly.villagetale.zone.type.AbstractZoneType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class FilterConfigurationScreen extends AbstractLedgerScreen {
    private static final int LIST_TOP = 36;
    private static final int LIST_BOTTOM = 145;
    private static final int TRASH_ICON_SIZE = 16;

    private final UUID villageId;
    private final UUID zoneId;
    private final FilterType filterType;
    private final int returnToZoneIndex;
    private FilterList filterList;
    private LedgerBackButton backButton;
    private Button clearAllButton;
    private Set<ResourceLocation> selectedFilters;

    public enum FilterType {
        ITEM,
        ENTITY
    }

    public FilterConfigurationScreen(UUID villageId, UUID zoneId, FilterType filterType, int returnToZoneIndex) {
        super(Component.translatable(filterType == FilterType.ITEM
                ? "villagetale.gui.filter.configure_item_filters"
                : "villagetale.gui.filter.configure_entity_filters"));
        this.villageId = villageId;
        this.zoneId = zoneId;
        this.filterType = filterType;
        this.returnToZoneIndex = returnToZoneIndex;
        this.selectedFilters = new HashSet<>();
    }

    @Override
    protected void init() {
        super.init();

        int leftPos = getLeftPos();
        int topPos = getTopPos();

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
            this.minecraft,
            CONTENT_WIDTH,
            LIST_BOTTOM - LIST_TOP,
            topPos + LIST_TOP,
            topPos + LIST_BOTTOM,
            12,
            entries
        );
        this.filterList.setLeftPos(leftPos + CONTENT_LEFT_MARGIN);
        this.addWidget(this.filterList);

        this.clearAllButton = this.addRenderableWidget(Button.builder(
            Component.translatable("villagetale.gui.filter.clear_all"),
            button -> clearAllFilters()
        ).bounds(leftPos + CONTENT_LEFT_MARGIN + 70, topPos + 15, 50, 12).build());

        this.backButton = this.addRenderableWidget(new LedgerBackButton(
            leftPos + 62,
            topPos + 153,
            button -> returnToZoneDetail()
        ));
    }

    @Override
    protected void renderLedgerContent(GuiGraphics guiGraphics, int leftPos, int topPos, int mouseX, int mouseY, float partialTick) {
        IVillageCapability village = VillageDataManager.getInstance().getVillageData(villageId);
        if (village == null) {
            guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.error.village_not_found"), leftPos + CONTENT_LEFT_MARGIN, topPos + 28, 0x3F3F3F, false);
            return;
        }

        IVillageZone zone = village.getZones().stream()
            .filter(z -> z.getUUID().equals(zoneId))
            .findFirst()
            .orElse(null);

        if (zone == null) {
            guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.error.zone_not_found"), leftPos + CONTENT_LEFT_MARGIN, topPos + 28, 0x3F3F3F, false);
            return;
        }

        Component title = Component.translatable(filterType == FilterType.ITEM
            ? "villagetale.gui.filter.configure_item_filters"
            : "villagetale.gui.filter.configure_entity_filters");
        guiGraphics.drawString(this.font, Component.literal(zone.getName()), leftPos + CONTENT_LEFT_MARGIN, topPos + 16, 0x3F3F3F, false);
        guiGraphics.drawString(this.font, title, leftPos + CONTENT_LEFT_MARGIN, topPos + 28, 0, false);

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

    private void returnToZoneDetail() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new ZoneDetailScreen(villageId, returnToZoneIndex));
        }
    }

    private void sendFilterUpdate() {
        UpdateZoneFilters.FilterType type = filterType == FilterType.ITEM
            ? UpdateZoneFilters.FilterType.ITEM
            : UpdateZoneFilters.FilterType.ENTITY;
        UpdateZoneFilters.send(villageId, zoneId, type, new ArrayList<>(selectedFilters));
    }

    private class FilterEntry {
        private final ResourceLocation id;
        private final String displayName;

        public FilterEntry(ResourceLocation id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }
    }

    private class FilterList extends ObjectSelectionList<FilterList.Entry> {
        private final List<FilterEntry> filterEntries;

        public FilterList(net.minecraft.client.Minecraft minecraft, int width, int height, int y, int bottom, int itemHeight, List<FilterEntry> entries) {
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
            return CONTENT_WIDTH;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getRowLeft() + this.getRowWidth() - 6;
        }

        public void updateCheckboxStates() {
            this.children().forEach(Entry::updateCheckboxState);
        }

        public class Entry extends ObjectSelectionList.Entry<Entry> {
            private final FilterEntry filterEntry;
            private CompactCheckbox checkbox;

            public Entry(FilterEntry filterEntry) {
                this.filterEntry = filterEntry;
                this.checkbox = new CompactCheckbox(
                    0,
                    0,
                    CONTENT_WIDTH - 10,
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
                        CONTENT_WIDTH - 10,
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
