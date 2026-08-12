package slimeknights.mantle.recipe.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import slimeknights.mantle.recipe.MantleRecipes;

import java.util.List;
import java.util.stream.Collectors;

/** Shaped recipe which fails to match if any of a list of other recipes matches, letting a general recipe defer to specific ones. */
@SuppressWarnings("WeakerAccess")
public class ShapedFallbackRecipe extends ShapedRecipe {
  /** Recipes to skip if they match */
  private final List<ResourceLocation> alternatives;
  private List<CraftingRecipe> alternativeCache;

  /**
   * Main constructor, creates a recipe from all parameters
   * @param group          Recipe group
   * @param category       Recipe book category
   * @param pattern        Recipe pattern
   * @param result         Recipe output
   * @param alternatives   List of recipe names to fail this match if they match
   */
  public ShapedFallbackRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result, boolean showNotification, List<ResourceLocation> alternatives) {
    super(group, category, pattern, result, showNotification);
    this.alternatives = alternatives;
  }

  /**
   * Creates a recipe using a shaped recipe as a base
   * @param base          Shaped recipe to copy data from
   * @param alternatives  List of recipe names to fail this match if they match
   */
  public ShapedFallbackRecipe(ShapedRecipe base, List<ResourceLocation> alternatives) {
    this(base.getGroup(), base.category(), base.pattern, base.result, base.showNotification(), alternatives);
  }

  @Override
  public boolean matches(CraftingInput input, Level world) {
    // if this recipe does not match, fail it
    if (!super.matches(input, world)) {
      return false;
    }

    // fetch all alternatives, fail if any match
    // cache to save effort down the line
    if (alternativeCache == null) {
      RecipeManager manager = world.getRecipeManager();
      alternativeCache = alternatives.stream()
                                     .map(manager::byKey)
                                     .filter(java.util.Optional::isPresent)
                                     .map(holder -> holder.get().value())
                                     .filter(recipe -> {
                                       // only allow exact shaped or shapeless match, prevent infinite recursion due to complex recipes
                                       Class<?> clazz = recipe.getClass();
                                       return clazz == ShapedRecipe.class || clazz == ShapelessRecipe.class;
                                     })
                                     .map(recipe -> (CraftingRecipe) recipe).collect(Collectors.toList());
    }
    // fail if any alternative matches
    return this.alternativeCache.stream().noneMatch(recipe -> recipe.matches(input, world));
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return MantleRecipes.CRAFTING_SHAPED_FALLBACK.get();
  }

  public static class Serializer implements RecipeSerializer<ShapedFallbackRecipe> {
    private static final Codec<List<ResourceLocation>> ALTERNATIVES = ResourceLocation.CODEC.listOf();

    public static final MapCodec<ShapedFallbackRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::getGroup),
      CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapedRecipe::category),
      ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.pattern),
      ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
      Codec.BOOL.optionalFieldOf("show_notification", Boolean.TRUE).forGetter(ShapedRecipe::showNotification),
      ALTERNATIVES.fieldOf("alternatives").forGetter(recipe -> recipe.alternatives)
    ).apply(instance, ShapedFallbackRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf,ShapedFallbackRecipe> STREAM_CODEC = StreamCodec.of(
      (buffer, recipe) -> {
        RecipeSerializer.SHAPED_RECIPE.streamCodec().encode(buffer, recipe);
        ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buffer, recipe.alternatives);
      },
      buffer -> {
        ShapedRecipe base = RecipeSerializer.SHAPED_RECIPE.streamCodec().decode(buffer);
        return new ShapedFallbackRecipe(base, ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buffer));
      });

    @Override
    public MapCodec<ShapedFallbackRecipe> codec() {
      return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf,ShapedFallbackRecipe> streamCodec() {
      return STREAM_CODEC;
    }
  }
}
