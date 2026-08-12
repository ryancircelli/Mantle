package slimeknights.mantle.recipe.helper;

import com.mojang.serialization.MapCodec;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMapBuilder;

import java.util.function.Supplier;

/**
 * Recipe serializer instance using loadables.
 * <p>
 * The serializer supplies a fixed parsing context to both of its codecs, holding itself under {@link #SERIALIZER} and,
 * for a {@link TypeAware} serializer, its type under {@link #TYPE}. There is no {@code ContextKey.ID} entry: 1.21 moved
 * the recipe ID out of the recipe and onto {@link net.minecraft.world.item.crafting.RecipeHolder}, so a recipe never
 * sees its own ID at parse time. A recipe which needs to name itself in a message should use its serializer's registry
 * name, which is what this class does for {@link ContextKey#DEBUG}.
 * @param <T>  Recipe type
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class LoadableRecipeSerializer<T extends Recipe<?>> implements LoggingRecipeSerializer<T> {
  /** Context key to use if you want the recipe serializer passed into your recipe */
  public static final ContextKey<RecipeSerializer<?>> SERIALIZER = new ContextKey<>("serializer");
  /** Context key to use if you want a type aware serializer in the recipe, requires {@link #of(RecordLoadable, Supplier)} for your serializer. */
  public static final ContextKey<TypeAwareRecipeSerializer<?>> TYPED_SERIALIZER = new ContextKey<>("typed_serializer");
  /** Context key to use if you want the recipe type passed into your recipe, requires {@link #of(RecordLoadable, Supplier)} for your serializer. */
  public static final ContextKey<RecipeType<?>> TYPE = new ContextKey<>("type");
  /** Field for a group key in a recipe (common requirement) */
  public static final LoadableField<String,Recipe<?>> RECIPE_GROUP = StringLoadable.DEFAULT.defaultField("group", "", Recipe::getGroup);


  protected final RecordLoadable<T> loadable;
  /** Codecs are built on first use rather than in the constructor, as {@link #buildContext()} is overridden by
   * subclasses whose own fields are not yet assigned while the constructor runs. */
  private MapCodec<T> codec;
  private StreamCodec<RegistryFriendlyByteBuf,T> streamCodec;

  /** Creates a standard serializer from a loadable */
  public static <T extends Recipe<?>> RecipeSerializer<T> of(RecordLoadable<T> loadable) {
    return new LoadableRecipeSerializer<>(loadable);
  }

  /** Creates a type aware serializer from a loadable */
  public static <T extends R, R extends Recipe<?>> TypeAwareRecipeSerializer<T> of(RecordLoadable<T> loadable, Supplier<? extends RecipeType<R>> type) {
    return new TypeAware<>(loadable, type);
  }

  /** Creates a serializer that is deprecated, logging a warning when used */
  public static <T extends Recipe<?>> RecipeSerializer<T> deprecated(RecordLoadable<T> loadable, String replacement) {
    return new Deprecated<>(loadable, replacement);
  }

  /** Builds the context handed to both codecs. Called once, lazily, on first parse. */
  protected TypedMapBuilder buildContext() {
    return TypedMapBuilder.builder().put(ContextKey.DEBUG, "Recipe serializer " + BuiltInRegistries.RECIPE_SERIALIZER.getKey(this)).put(SERIALIZER, this);
  }

  @Override
  public MapCodec<T> codec() {
    if (codec == null) {
      codec = loadable.mapCodec(buildContext().build());
    }
    return codec;
  }

  @Override
  public StreamCodec<RegistryFriendlyByteBuf,T> streamCodecSafe() {
    if (streamCodec == null) {
      streamCodec = loadable.streamCodec(buildContext().build());
    }
    return streamCodec;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + '[' + loadable + ']';
  }

  /**
   * Serializer for a recipe class shared by several recipe types.
   * <p>
   * 1.21's serializer sees nothing but the recipe JSON at parse, so the type cannot come out of the file. It comes from
   * the serializer instance instead: register one instance per type over the same loadable, exactly as vanilla registers
   * one {@code SimpleCookingSerializer} per cooking type, and the recipe reads it back out of the context.
   */
  public static class TypeAware<T extends Recipe<?>> extends LoadableRecipeSerializer<T> implements TypeAwareRecipeSerializer<T> {
    private final Supplier<? extends RecipeType<?>> type;
    protected TypeAware(RecordLoadable<T> loadable, Supplier<? extends RecipeType<?>> type) {
      super(loadable);
      this.type = type;
    }

    @Override
    protected TypedMapBuilder buildContext() {
      return super.buildContext().put(TYPE, getType()).put(TYPED_SERIALIZER, this);
    }

    @Override
    public RecipeType<?> getType() {
      return type.get();
    }
  }

  /** Helper class that logs a warning on recipe parse about planned removal */
  private static class Deprecated<T extends Recipe<?>> extends LoadableRecipeSerializer<T> {
    private final String replacement;
    protected Deprecated(RecordLoadable<T> loadable, String replacement) {
      super(loadable);
      this.replacement = replacement;
    }

    /** @implNote  Warns from the codec fetch rather than from the parse itself, as 1.21 gives a serializer no hook
     *             between the two. The recipe cannot be named as it no longer knows its own ID. */
    @Override
    public MapCodec<T> codec() {
      Mantle.logger.warn("Using deprecated recipe serializer {}, {}", BuiltInRegistries.RECIPE_SERIALIZER.getKey(this), replacement);
      return super.codec();
    }
  }
}
