package org.sosly.villagetale.compat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import net.minecraftforge.fml.ModList;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IRecipeManager;
import org.sosly.villagetale.compat.jei.JEICompat;

public class CompatRegistry {
    private static final Map<String, Supplier<Callable<ICompat>>> compatFactories = new HashMap<>();
    private static final Map<String, ICompat> loadedCompats = new HashMap<>();

    static {
        compatFactories.put(CompatModIDs.JEI, () -> JEICompat::new);
    }

    public static void registerCompats() {
        for (Map.Entry<String, Supplier<Callable<ICompat>>> entry : compatFactories.entrySet()) {
            if (!ModList.get().isLoaded(entry.getKey())) {
                continue;
            }

            try {
                ICompat compat = entry.getValue().get().call();
                compat.setup();
                loadedCompats.put(entry.getKey(), compat);
            } catch (Exception e) {
                VillageTale.LOGGER.error("Error instantiating compatibility:", e);
            }
        }
    }

    public static IRecipeManager getRecipeManager() {
        if (ModList.get().isLoaded(CompatModIDs.JEI)) {
            ICompat compat = loadedCompats.get(CompatModIDs.JEI);
            if (compat instanceof IRecipeManager recipeManager) {
                return recipeManager;
            }
        }

        return null;
    }
}
