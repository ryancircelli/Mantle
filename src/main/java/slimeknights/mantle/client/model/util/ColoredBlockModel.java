package slimeknights.mantle.client.model.util;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Transformation;
import lombok.Getter;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel.Builder;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import org.joml.Vector3f;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.common.ColorLoadable;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.LogicHelper;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

/**
 * Block model for setting color, luminosity, and per element uv lock. Similar to {@link MantleItemLayerModel} but for blocks
 */
@SuppressWarnings("unused")  // API
public class ColoredBlockModel extends SimpleBlockModel {
  /** Model loader to allow doing basic coloring outside of other models */
  public static final IGeometryLoader<SimpleBlockModel> LOADER = ColoredBlockModel::deserialize;
  /**
   * Face bakery for this model's quads. {@link net.minecraft.client.renderer.block.model.BlockModel#FACE_BAKERY} is
   * private; the class is stateless with a public constructor, so owning an instance costs nothing and is what
   * NeoForge's own {@code UnbakedGeometryHelper} does.
   */
  private static final FaceBakery FACE_BAKERY = new FaceBakery();

  /** Colors to use for each piece */
  @Getter
  private final List<ColorData> colorData;

  /**
   * Creates a new colored block model
   * @param parentLocation Location of the parent model, if unset has no parent
   * @param textures       List of textures for iteration, in case the owner is not BlockModel
   * @param parts          List of parts in the model
   * @param colorData      Additional information about colors in the model
   */
  public ColoredBlockModel(@Nullable ResourceLocation parentLocation, Map<String,Either<Material,String>> textures, List<BlockElement> parts, List<ColorData> colorData) {
    super(parentLocation, textures, parts);
    this.colorData = colorData;
  }

  public ColoredBlockModel(SimpleBlockModel base, List<ColorData> colorData) {
    super(base);
    this.colorData = colorData;
  }

  /**
   * Bakes a single part of the model into the builder
   * @param builder          Baked model builder
   * @param owner            Model owner
   * @param part             Part to bake
   * @param emissivity       Emissivity for fullbright, -1 will leave the face's own {@code neoforge_data} in charge, 0-15 will override it
   * @param spriteGetter     Sprite getter
   * @param transform        Transform for the face, including the UV lock this part should bake with
   * @param quadTransformer  NeoForge transformations for the face, this is notably where you should handle color transformations
   */
  public static void bakePart(Builder builder, IGeometryBakingContext owner, BlockElement part, int emissivity, Function<Material,TextureAtlasSprite> spriteGetter, ModelState transform, IQuadTransformer quadTransformer) {
    for (Entry<Direction, BlockElementFace> entry : part.faces.entrySet()) {
      BlockElementFace face = entry.getValue();
      // ensure the name is not prefixed (it always is)
      String texture = face.texture();
      if (texture.charAt(0) == '#') {
        texture = texture.substring(1);
      }
      // bake the face with the extra colors
      TextureAtlasSprite sprite = spriteGetter.apply(owner.getMaterial(texture));
      BakedQuad quad = bakeFace(part, face, sprite, entry.getKey(), transform, emissivity);
      quadTransformer.processInPlace(quad);
      // apply cull face
      if (face.cullForDirection() == null) {
        builder.addUnculledFace(quad);
      } else {
        builder.addCulledFace(Direction.rotate(transform.getRotation().getMatrix(), face.cullForDirection()), quad);
      }
    }
  }

  /**
   * Bakes a list of block part elements into a model
   * @param owner         Model configuration
   * @param elements      Model elements
   * @param spriteGetter  Sprite getter instance
   * @param transform     Model transform
   * @param overrides     Model overrides
   * @return  Baked model
   */
  public static BakedModel bakeModel(IGeometryBakingContext owner, List<BlockElement> elements, List<ColorData> colorData, Function<Material,TextureAtlasSprite> spriteGetter, ModelState transform, ItemOverrides overrides) {
    // iterate parts, adding to the builder
    TextureAtlasSprite particle = spriteGetter.apply(owner.getMaterial("particle"));
    SimpleBakedModel.Builder builder = bakedBuilder(owner, overrides).particle(particle);
    int size = elements.size();
    IQuadTransformer quadTransformer = applyTransform(transform, owner.getRootTransform());
    boolean uvlock = transform.isUvLocked();
    for (int i = 0; i < size; i++) {
      BlockElement part = elements.get(i);
      ColorData colors = LogicHelper.getOrDefault(colorData, i, ColorData.DEFAULT);
      IQuadTransformer partTransformer = colors.color == -1 ? quadTransformer : quadTransformer.andThen(applyColorQuadTransformer(colors.color));
      bakePart(builder, owner, part, colors.luminosity, spriteGetter, UvLockedState.of(transform, colors.isUvLock(uvlock)), partTransformer);
    }
    return builder.build(getRenderTypeGroup(owner));
  }

  @Override
  public BakedModel bake(IGeometryBakingContext owner, ModelBaker baker, Function<Material,TextureAtlasSprite> spriteGetter, ModelState modelTransform, ItemOverrides overrides) {
    return bakeModel(owner, getElements(), colorData, spriteGetter, modelTransform, overrides);
  }

  @Override
  public BakedModel bakeWithElements(IGeometryBakingContext owner, List<BlockElement> elements, ModelState transform) {
    return bakeModel(owner, elements, colorData, Material::sprite, transform, ItemOverrides.EMPTY);
  }

  /**
   * Model state wrapper overriding just the UV lock, which is how a single element opts in or out of UV lock now that
   * {@link net.minecraft.client.renderer.block.model.FaceBakery#bakeQuad} reads the flag off the state instead of
   * taking it as an argument. {@link net.neoforged.neoforge.client.model.SimpleModelState} is not a substitute: it
   * would also answer {@link ModelState#mayApplyArbitraryRotation()} for itself, and the face bakery reads that to
   * decide whether a quad's winding may be recalculated.
   */
  public record UvLockedState(ModelState base, boolean uvLock) implements ModelState {
    @Override
    public Transformation getRotation() {
      return base.getRotation();
    }

    @Override
    public boolean isUvLocked() {
      return uvLock;
    }

    @Override
    public boolean mayApplyArbitraryRotation() {
      return base.mayApplyArbitraryRotation();
    }

    /** Gets a state with the given UV lock, returning the base state when it already agrees */
    public static ModelState of(ModelState base, boolean uvLock) {
      return base.isUvLocked() == uvLock ? base : new UvLockedState(base, uvLock);
    }
  }

  /**
   * Data class for setting properties when baking colored elements
   * @param color       Color to multiply into the element, -1 for none
   * @param luminosity  Emissivity override for the element, -1 to leave the element's own {@code neoforge_data} in
   *                    charge. Kept because it is per element in this loader's own {@code colors} array, where
   *                    {@code neoforge_data} has to be written onto every element it applies to.
   * @param uvlock      UV lock override for the element, null to inherit the block state's
   */
  public record ColorData(int color, int luminosity, @Nullable Boolean uvlock) {
    public static final ColorData DEFAULT = new ColorData(-1, -1, null);
    public static final RecordLoadable<ColorData> LOADABLE = RecordLoadable.create(
      ColorLoadable.ALPHA.defaultField("color", false, ColorData::color),
      IntLoadable.range(-1, 15).defaultField("luminosity", -1, ColorData::luminosity),
      BooleanLoadable.INSTANCE.nullableField("uvlock", ColorData::uvlock),
      ColorData::new);
    public static final Loadable<List<ColorData>> LIST_LOADABLE = LOADABLE.list(0);

    /** Gets the UV lock for the given part */
    public boolean isUvLock(boolean defaultLock) {
      if (uvlock == null) {
        return defaultLock;
      }
      return uvlock;
    }

    /** @deprecated use {@link #LOADABLE} */
    @Deprecated(forRemoval = true)
    public static ColorData fromJson(JsonObject json) {
      return LOADABLE.deserialize(json);
    }

    /** @deprecated use {@link #LOADABLE} */
    @Deprecated(forRemoval = true)
    public JsonObject toJson() {
      JsonObject json = new JsonObject();
      LOADABLE.serialize(this, json);
      return json;
    }
  }


  /* Deserializing */

  /** Deserializes the model from JSON */
  public static ColoredBlockModel deserialize(JsonObject json, JsonDeserializationContext context) {
    SimpleBlockModel model = SimpleBlockModel.deserialize(json, context);
    List<ColorData> colorData = ColorData.LIST_LOADABLE.getOrDefault(json, "colors", List.of());
    return new ColoredBlockModel(model, colorData);
  }


  /* Face bakery */

  /**
   * Converts an ARGB color to an ABGR color, as the commonly used color format is not the format colors end up packed into.
   * This function doubles as its own inverse, not that its needed.
   * @param color  ARGB color
   * @return  ABGR color
   * @see QuadTransformers#toABGR(int)
   */
  public static int swapColorRedBlue(int color) {
    return QuadTransformers.toABGR(color);
  }

  /**
   * Quad transformer applying a static color
   * @see QuadTransformers#applyingColor(int)
   */
  public static IQuadTransformer applyColorQuadTransformer(int color) {
    return QuadTransformers.applyingColor(color);
  }

  /**
   * Extension of {@link net.minecraft.client.renderer.block.model.BlockModel#bakeFace} with an emissivity argument
   * @param part        Part containing the face
   * @param face        Face data
   * @param sprite      Sprite for the face
   * @param facing      Direction of the face
   * @param transform   Transform for the face, including the UV lock to bake with
   * @param emissivity  Emissivity for fullbright, -1 will leave the face's own {@code neoforge_data} in charge, 0-15 will override it
   */
  public static BakedQuad bakeFace(BlockElement part, BlockElementFace face, TextureAtlasSprite sprite, Direction facing, ModelState transform, int emissivity) {
    return bakeQuad(part.from, part.to, face, sprite, facing, transform, part.rotation, part.shade, emissivity);
  }

  /**
   * Extension of {@link net.minecraft.client.renderer.block.model.FaceBakery#bakeQuad} with an emissivity override.
   * <p>
   * The UV lock is supplied through the {@link ModelState} (see {@link UvLockedState}); the face bakery applies a
   * face's own {@code neoforge_data} light and color itself, leaving only the emissivity override to do here.
   * @param posFrom        Face start position
   * @param posTo          Face end position
   * @param face           Face data
   * @param sprite         Sprite for the face
   * @param facing         Direction of the face
   * @param transform      Transform for the face, including the UV lock to bake with
   * @param partRotation   Rotation for the part
   * @param shade          If true, shades the part
   * @param emissivity     Emissivity for fullbright, -1 will leave the face's own {@code neoforge_data} in charge, 0-15 will override it
   * @return  Baked quad
   */
  public static BakedQuad bakeQuad(Vector3f posFrom, Vector3f posTo, BlockElementFace face, TextureAtlasSprite sprite,
                                   Direction facing, ModelState transform, @Nullable BlockElementRotation partRotation,
                                   boolean shade, int emissivity) {
    BakedQuad quad = FACE_BAKERY.bakeQuad(posFrom, posTo, face, sprite, facing, transform, partRotation, shade);
    if (emissivity > 0) {
      QuadTransformers.settingEmissivity(emissivity).processInPlace(quad);
    }
    return quad;
  }
}
