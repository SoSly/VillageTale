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
import org.sosly.villagetale.data.ItemOrTagMatcher;

public abstract class AbstractZoneType implements IZoneType {
    private String claimType;
    private final BlockOrTagMatcher blockMatcher = new BlockOrTagMatcher();
    private final Set<ResourceLocation> entityTypes = new HashSet<>();
    private final ItemOrTagMatcher itemFilter = new ItemOrTagMatcher();
    private final Set<ResourceLocation> entityFilter = new HashSet<>();

    public String getClaimType() {
        return claimType;
    }

    public Set<ResourceLocation> getAcceptedEntityTypes() {
        if ("entity".equals(claimType)) {
            return entityTypes;
        }
        return new HashSet<>();
    }

    public ItemOrTagMatcher getItemFilter() {
        return itemFilter;
    }

    public Set<ResourceLocation> getEntityFilter() {
        return entityFilter;
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
            case "entity" -> loadEntityTypes(claims.getAsJsonArray("values"));
        }
    }

    private void loadEntityTypes(JsonElement values) {
        entityTypes.clear();
        for (JsonElement value : values.getAsJsonArray()) {
            entityTypes.add(new ResourceLocation(value.getAsString()));
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
            case "entity" -> loadEntityFilter(values);
        }
    }

    private void loadEntityFilter(JsonElement values) {
        entityFilter.clear();
        for (JsonElement value : values.getAsJsonArray()) {
            entityFilter.add(new ResourceLocation(value.getAsString()));
        }
    }
}
