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
import org.sosly.villagetale.zone.type.Tannery;
import org.sosly.villagetale.zone.type.TownHall;
import org.sosly.villagetale.zone.type.Woodshop;

public class ZoneRegistry {
    public static final ZoneRegistry INSTANCE = new ZoneRegistry();

    private final Map<ResourceLocation, Supplier<IZoneShape>> shapes = new HashMap<>();
    private final Map<ResourceLocation, IZoneType> types = new HashMap<>();
    private ZoneRegistry() {}

    public void register(ResourceLocation id, IZoneType type) {
        types.put(id, type);
        VillageTale.LOGGER.info("Registered zone type: {} (total: {})", id, types.size());
    }

    public IZoneType type(ResourceLocation id) {
        IZoneType type = types.get(id);
        if (type == null) {
            VillageTale.LOGGER.warn("Failed to find zone type: {} (available: {})", id, types.keySet());
            return null;
        }
        return type;
    }

    public Iterable<ResourceLocation> getZoneTypeIDs() {
        return types.keySet();
    }

    public IZoneShape shape(ResourceLocation id) {
        Supplier<IZoneShape> supplier = shapes.get(id);
        if (supplier == null) {
            return null;
        }
        return supplier.get();
    }

    {
        // todo: this is a hack because the events are not working for some reason.
        types.put(Butchery.ID, new Butchery());
        types.put(Farmland.ID, new Farmland());
        types.put(Forest.ID, new Forest());
        types.put(Home.ID, new Home());
        types.put(Kitchen.ID, new Kitchen());
        types.put(Pen.ID, new Pen());
        types.put(Storage.ID, new Storage());
        types.put(Tannery.ID, new Tannery());
        types.put(TownHall.ID, new TownHall());
        types.put(Woodshop.ID, new Woodshop());
        // end todo

        shapes.put(Point.ID, Point::new);
        shapes.put(Box.ID, Box::new);
        shapes.put(Route.ID, Route::new);
        shapes.put(Cylinder.ID, Cylinder::new);
    }
}
