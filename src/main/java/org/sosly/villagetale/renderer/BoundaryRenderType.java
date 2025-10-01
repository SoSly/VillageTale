package org.sosly.villagetale.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BoundaryRenderType extends RenderType {
    private static final RenderType BOUNDARY_LINES = create(
        "villagetale_boundary_lines",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.LINES,
        256,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_LINES_SHADER)
            .setLineState(new RenderStateShard.LineStateShard(java.util.OptionalDouble.of(5.0)))
            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setOutputState(ITEM_ENTITY_TARGET)
            .setWriteMaskState(COLOR_DEPTH_WRITE)
            .setCullState(NO_CULL)
            .setDepthTestState(NO_DEPTH_TEST)
            .createCompositeState(false)
    );

    private BoundaryRenderType(String pName, VertexFormat pFormat, VertexFormat.Mode pMode, int pBufferSize, boolean pAffectsCrumbling, boolean pSortOnUpload, Runnable pSetupState, Runnable pClearState) {
        super(pName, pFormat, pMode, pBufferSize, pAffectsCrumbling, pSortOnUpload, pSetupState, pClearState);
    }

    public static RenderType boundaryLines() {
        return BOUNDARY_LINES;
    }
}
