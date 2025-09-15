package org.sosly.villageworks;

import net.minecraft.client.Minecraft;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(VillageWorks.MOD_ID)
@PrefixGameTestTemplate(false)
public class VillageWorksTest {
    @GameTest(template = "empty")
    public static void modIsLoaded(GameTestHelper helper) {
        helper.succeedIf(() -> ModList.get().isLoaded(VillageWorks.MOD_ID));
    }
}
