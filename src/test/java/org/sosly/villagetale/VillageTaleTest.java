package org.sosly.villagetale;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.sosly.villagetale.VillageTale;

@GameTestHolder(VillageTale.MOD_ID)
@PrefixGameTestTemplate(false)
public class VillageTaleTest {
    @GameTest(template = "empty")
    public static void modIsLoaded(GameTestHelper helper) {
        helper.succeedIf(() -> ModList.get().isLoaded(VillageTale.MOD_ID));
    }
}
