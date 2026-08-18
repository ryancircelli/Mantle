package slimeknights.mantle.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import slimeknights.mantle.Mantle;

/**
 * Class for render types defined by Mantle
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MantleRenderTypes {
  /** Extension of {@link RenderType#POSITION_COLOR_TEX_LIGHTMAP_SHADER} with fog information based on {@link RenderType#ENTITY_TRANSLUCENT_CULL} */
  public static final RenderStateShard.ShaderStateShard FLUID_SHADER = new RenderStateShard.ShaderStateShard(MantleShaders::getConfiguredFluidShader);

  /**
   * Render type used for the fluid renderer.
   * TODO 1.21: can we replace this with {@link RenderType#ENTITY_TRANSLUCENT_CULL}? Would require including normals in our vertex format.
   */
  public static final RenderType FLUID = RenderType.create(
    Mantle.modId + ":block_render_type",
    DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, Mode.QUADS, 256, false, true,
    RenderType.CompositeState.builder()
      .setLightmapState(RenderStateShard.LIGHTMAP)
      .setShaderState(FLUID_SHADER)
      .setTextureState(RenderStateShard.BLOCK_SHEET_MIPPED)
      .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
      .createCompositeState(false));

  /**
   * Vertex format used for the structure renderer.
   * 1.20 built this by hand out of {@code DefaultVertexFormat}'s element constants, which are gone: elements moved to
   * {@link com.mojang.blaze3d.vertex.VertexFormatElement} and formats are built through {@link VertexFormat#builder()}.
   * {@link DefaultVertexFormat#NEW_ENTITY} is element for element the format this used to declare, so it is that now
   * rather than a rebuilt copy of it.
   */
  public static final VertexFormat BLOCK_WITH_OVERLAY = DefaultVertexFormat.NEW_ENTITY;

  public static final RenderType TRANSLUCENT_FULLBRIGHT = RenderType.create(
    Mantle.modId + ":translucent_fullbright",
    BLOCK_WITH_OVERLAY, Mode.QUADS, 256, false, false,
    RenderType.CompositeState.builder()
      .setShaderState(new RenderStateShard.ShaderStateShard(MantleShaders::getBlockFullBrightShader))
      .setLightmapState(new RenderStateShard.LightmapStateShard(false))
      .setOverlayState(RenderStateShard.OVERLAY)
      .setTextureState(RenderStateShard.BLOCK_SHEET_MIPPED)
      .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
      .createCompositeState(false));
}
