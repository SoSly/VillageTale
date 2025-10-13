package org.sosly.villagetale.compat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import net.minecraftforge.fml.ModList;
import org.sosly.villagetale.VillageTale;

public class CompatRegistry {
    private static final Map<String, Supplier<Callable<ICompat>>> COMPAT_FACTORIES = new HashMap<>();
    private static final Map<String, ICompat> LOADED_COMPATS = new HashMap<>();

    public static void registerCompats() {
        for (Map.Entry<String, Supplier<Callable<ICompat>>> entry : COMPAT_FACTORIES.entrySet()) {
            if (!ModList.get().isLoaded(entry.getKey())) {
                continue;
            }

            try {
                ICompat compat = entry.getValue().get().call();
                compat.setup();
                LOADED_COMPATS.put(entry.getKey(), compat);
            } catch (Exception e) {
                VillageTale.LOGGER.error("Error instantiating compatibility:", e);
            }
        }
    }
}
