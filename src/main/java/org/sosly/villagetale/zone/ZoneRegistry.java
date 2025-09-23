package org.sosly.villagetale.zone;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IZoneShape;
import org.sosly.villagetale.api.IZoneType;
import org.sosly.villagetale.zone.shape.Point;
import org.sosly.villagetale.zone.shape.Rectangle;
import org.sosly.villagetale.zone.shape.Route;
import org.sosly.villagetale.zone.shape.Sphere;
import org.sosly.villagetale.zone.type.Farmland;
import org.sosly.villagetale.zone.type.Home;
import org.sosly.villagetale.zone.type.Kitchen;
import org.sosly.villagetale.zone.type.Storage;
import org.sosly.villagetale.zone.type.TownHall;

public class ZoneRegistry {
    public static final ZoneRegistry INSTANCE = new ZoneRegistry();

    private final Map<ResourceLocation, Supplier<IZoneShape>> SHAPES = new HashMap<>();
    private final Map<ResourceLocation, Supplier<IZoneType>> TYPES = new HashMap<>();
    private ZoneRegistry() {}

    public void register(ResourceLocation id, Supplier<IZoneType> supplier) {
        TYPES.put(id, supplier);
        VillageTale.LOGGER.info("Registered zone type: {} (total: {})", id, TYPES.size());
    }

    public IZoneType type(ResourceLocation id) {
        Supplier<IZoneType> supplier = TYPES.get(id);
        if (supplier == null) {
            VillageTale.LOGGER.warn("Failed to find zone type: {} (available: {})", id, TYPES.keySet());
            return null;
        }
        return supplier.get();
    }

    public Iterable<ResourceLocation> getZoneTypeIDs() {
        return TYPES.keySet();
    }

    public IZoneShape shape(ResourceLocation id) {
        Supplier<IZoneShape> supplier = SHAPES.get(id);
        if (supplier == null) {
            return null;
        }
        return supplier.get();
    }

    {
        // todo: this is a hack because the events are not working for some reason.
        TYPES.put(Farmland.ID, Farmland::new);
        TYPES.put(Home.ID, Home::new);
        TYPES.put(Kitchen.ID, Kitchen::new);
        TYPES.put(Storage.ID, Storage::new);
        TYPES.put(TownHall.ID, TownHall::new);
        // end todo

        SHAPES.put(Point.ID, Point::new);
        SHAPES.put(Rectangle.ID, Rectangle::new);
        SHAPES.put(Route.ID, Route::new);
        SHAPES.put(Sphere.ID, Sphere::new);
    }
}
