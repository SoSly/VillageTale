package org.sosly.villagetale.zone.type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.sosly.villagetale.api.IZoneShape;
import org.sosly.villagetale.api.IZoneType;
import org.sosly.villagetale.data.BlockOrTagMatcher;
import org.sosly.villagetale.data.EntityTypeOrTagMatcher;
import org.sosly.villagetale.data.ItemOrTagMatcher;

public abstract class AbstractZoneType implements IZoneType {
    private String claimType;
    private final BlockOrTagMatcher blockMatcher = new BlockOrTagMatcher();
    private final EntityTypeOrTagMatcher entityTypeMatcher = new EntityTypeOrTagMatcher();
    private final ItemOrTagMatcher itemFilter = new ItemOrTagMatcher();
    private final EntityTypeOrTagMatcher entityFilter = new EntityTypeOrTagMatcher();

    public String getClaimType() {
        return claimType;
    }

    public Set<ResourceLocation> getAcceptedEntityTypes() {
        if ("entity".equals(claimType)) {
            return new HashSet<>(entityTypeMatcher.getAllEntityTypeIds());
        }
        return new HashSet<>();
    }

    public ItemOrTagMatcher getItemFilter() {
        return itemFilter;
    }

    public Set<ResourceLocation> getEntityFilter() {
        return new HashSet<>(entityFilter.getAllEntityTypeIds());
    }

    @Override
    public void initialize(Level level, IZoneShape shape) {
    }

    @Override
    public boolean isPOI(Level level, BlockPos pos) {
        if ("block".equals(claimType)) {
            return blockMatcher.matches(level, pos);
        }

        if ("entity".equals(claimType)) {
            return true;
        }

        return false;
    }

    @Override
    public CompoundTag serializeNBT() {
        return null;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
    }

    public void onDatapackReload(JsonObject data) {
        if (data.has("claims")) {
            loadClaims(data.getAsJsonObject("claims"));
        }

        if (data.has("filters")) {
            loadFilters(data.getAsJsonArray("filters"));
        }
    }

    private void loadClaims(JsonObject claims) {
        if (claims.has("type")) {
            claimType = claims.get("type").getAsString();
        }

        if (!claims.has("values")) {
            return;
        }

        switch (claimType) {
            case "block" -> blockMatcher.loadFromJson(claims.getAsJsonArray("values"));
            case "entity" -> entityTypeMatcher.loadFromJson(claims.getAsJsonArray("values"));
        }
    }


    private void loadFilters(JsonElement filters) {
        for (JsonElement filterElement : filters.getAsJsonArray()) {
            loadFilter(filterElement.getAsJsonObject());
        }
    }

    private void loadFilter(JsonObject filter) {
        String filterType = filter.get("type").getAsString();
        JsonElement values = filter.get("values");

        switch (filterType) {
            case "item" -> itemFilter.loadFromJson(values.getAsJsonArray());
            case "entity" -> entityFilter.loadFromJson(values.getAsJsonArray());
        }
    }

}
