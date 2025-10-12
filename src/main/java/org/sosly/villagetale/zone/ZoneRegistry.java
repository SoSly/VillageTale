package org.sosly.villagetale.zone;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IZoneShape;
import org.sosly.villagetale.api.IZoneType;
import org.sosly.villagetale.zone.shape.Box;
import org.sosly.villagetale.zone.shape.Cylinder;
import org.sosly.villagetale.zone.shape.Point;
import org.sosly.villagetale.zone.shape.Route;
import org.sosly.villagetale.zone.type.Butchery;
import org.sosly.villagetale.zone.type.Farmland;
import org.sosly.villagetale.zone.type.Forest;
import org.sosly.villagetale.zone.type.Home;
import org.sosly.villagetale.zone.type.Kitchen;
import org.sosly.villagetale.zone.type.Pen;
import org.sosly.villagetale.zone.type.Storage;
import org.sosly.villagetale.zone.type.TownHall;
import org.sosly.villagetale.zone.type.Woodshop;

public class ZoneRegistry {
    public static final ZoneRegistry INSTANCE = new ZoneRegistry();

    private final Map<ResourceLocation, Supplier<IZoneShape>> SHAPES = new HashMap<>();
    private final Map<ResourceLocation, IZoneType> TYPES = new HashMap<>();
    private ZoneRegistry() {}

    public void register(ResourceLocation id, IZoneType type) {
        TYPES.put(id, type);
        VillageTale.LOGGER.info("Registered zone type: {} (total: {})", id, TYPES.size());
    }

    public IZoneType type(ResourceLocation id) {
        IZoneType type = TYPES.get(id);
        if (type == null) {
            VillageTale.LOGGER.warn("Failed to find zone type: {} (available: {})", id, TYPES.keySet());
            return null;
        }
        return type;
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
        TYPES.put(Butchery.ID, new Butchery());
        TYPES.put(Farmland.ID, new Farmland());
        TYPES.put(Forest.ID, new Forest());
        TYPES.put(Home.ID, new Home());
        TYPES.put(Kitchen.ID, new Kitchen());
        TYPES.put(Pen.ID, new Pen());
        TYPES.put(Storage.ID, new Storage());
        TYPES.put(TownHall.ID, new TownHall());
        TYPES.put(Woodshop.ID, new Woodshop());
        // end todo

        SHAPES.put(Point.ID, Point::new);
        SHAPES.put(Box.ID, Box::new);
        SHAPES.put(Route.ID, Route::new);
        SHAPES.put(Cylinder.ID, Cylinder::new);
    }
}
