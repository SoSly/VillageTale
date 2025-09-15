package org.sosly.villageworks;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(VillageWorks.MOD_ID)
public class VillageWorks {
    public static final String MOD_ID = "villageworks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VillageWorks() {
        LOGGER.info("Loading VillageWorks");
    }
}
