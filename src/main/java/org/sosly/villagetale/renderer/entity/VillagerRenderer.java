package org.sosly.villagetale.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.network.ClientPacketHandler;
import org.sosly.villagetale.profession.professions.Farmer;
import org.sosly.villagetale.renderer.model.VillagerModel;

@OnlyIn(Dist.CLIENT)
public class VillagerRenderer extends MobRenderer<Villager, VillagerModel<Villager>> {
    private static final ResourceLocation VILLAGER_BASE_SKIN =
        new ResourceLocation("minecraft", "textures/entity/villager/villager.png");
    public static final ModelLayerLocation VILLAGER_ARMS = new ModelLayerLocation(
        new ResourceLocation(VillageTale.MOD_ID, "villager_arms"), "main");

    public VillagerRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(VILLAGER_ARMS)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new VillagerProfessionLayer(this, context));
    }

    @Override
    public ResourceLocation getTextureLocation(Villager villager) {
        return VILLAGER_BASE_SKIN;
    }

    @OnlyIn(Dist.CLIENT)
    public static class VillagerProfessionLayer extends RenderLayer<Villager, VillagerModel<Villager>> {
        private static final ResourceLocation FARMER_OVERLAY =
            new ResourceLocation("minecraft", "textures/entity/villager/profession/farmer.png");

        public VillagerProfessionLayer(VillagerRenderer renderer, EntityRendererProvider.Context context) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                          Villager villager, float limbSwing, float limbSwingAmount,
                          float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            ResourceLocation professionId = ClientPacketHandler.getCachedProfession(villager.getId());
            if (professionId == null) {
                return;
            }

            if (!professionId.equals(Farmer.ID)) {
                return;
            }

            renderColoredCutoutModel(this.getParentModel(), FARMER_OVERLAY, poseStack, buffer,
                                    packedLight, villager, 1.0F, 1.0F, 1.0F);
        }
    }
}
