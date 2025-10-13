package org.sosly.villagetale.renderer.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.client.ClientDataManager;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.profession.ProfessionRegistry;
import org.sosly.villagetale.renderer.model.VillagerModel;

@OnlyIn(Dist.CLIENT)
public class VillagerRenderer extends MobRenderer<Villager, VillagerModel<Villager>> {
    private static final ResourceLocation VILLAGER_BASE_SKIN =
        new ResourceLocation(VillageTale.MOD_ID, "textures/entity/villager/profession/commoner.png");

    public VillagerRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(VillagerModel.LAYER_LOCATION)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(Villager villager) {
        ResourceLocation professionId = ClientDataManager.getCachedProfession(villager.getId());
        if (professionId != null) {
            return ProfessionRegistry.INSTANCE.getProfession(professionId)
                .map(profession -> new ResourceLocation(profession.getID().getNamespace(),
                    "textures/entity/villager/profession/" + profession.getID().getPath() + ".png"))
                .orElse(VILLAGER_BASE_SKIN);
        }
        return VILLAGER_BASE_SKIN;
    }

}
