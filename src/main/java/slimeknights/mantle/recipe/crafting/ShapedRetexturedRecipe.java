package slimeknights.mantle.recipe.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import slimeknights.mantle.recipe.MantleRecipes;
import slimeknights.mantle.util.RetexturedHelper;

import javax.annotation.Nullable;

/**
 * Recipe which sets the texture for a {@link slimeknights.mantle.block.RetexturedBlock} based on an ingredient input.
 * <p>
 * The texture is named by a symbol from the recipe's own key. 1.20 also accepted an inline ingredient object under the
 * same field, which warned "use key instead" on every parse; that form is gone, as the recipe cannot see its key map
 * without asking for it and there is no reason left to texture from something that is not an input.
 */
// TODO 1.21: rework to be more like the ShapedMaterialsRecipe from Tinkers for more efficient network syncing
@SuppressWarnings("WeakerAccess")
public class ShapedRetexturedRecipe extends ShapedRecipe {
  /** Key and pattern this recipe was read from, needed to write it back out. Null for a recipe read from the network. */
  @Nullable
  private final ShapedRecipePattern.Data data;
  /** Symbol in {@link #data}'s key naming {@link #texture}. Unset for a recipe read from the network. */
  private final char textureKey;
  /** Ingredient used to determine the texture on the output */
  @Getter
  private final Ingredient texture;
  private final boolean matchAll;

  /** Creates a new recipe using the passed parameters */
  protected ShapedRetexturedRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result, boolean showNotification,
                                   @Nullable ShapedRecipePattern.Data data, char textureKey, Ingredient texture, boolean matchAll) {
    super(group, category, pattern, result, showNotification);
    this.data = data;
    this.textureKey = textureKey;
    this.texture = texture;
    this.matchAll = matchAll;
  }

  /**
   * Creates a new recipe using an existing shaped recipe
   * @param orig       Shaped recipe to copy
   * @param texture    Ingredient to use for the texture
   * @param matchAll   If true, all inputs must match for the recipe to match
   */
  protected ShapedRetexturedRecipe(ShapedRecipe orig, Ingredient texture, boolean matchAll) {
    this(orig.getGroup(), orig.category(), orig.pattern, orig.result, orig.showNotification(), null, '\0', texture, matchAll);
  }

  /**
   * Gets the output using the given texture
   * @param texture  Texture to use
   * @return  Output with texture. Will be blank if the input is not a block
   */
  public ItemStack getResultItem(Item texture, HolderLookup.Provider registries) {
    return RetexturedHelper.setTexture(getResultItem(registries).copy(), Block.byItem(texture));
  }

  @Override
  public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
    ItemStack result = super.assemble(input, registries);
    Block currentTexture = null;
    for (int i = 0; i < input.size(); i++) {
      ItemStack stack = input.getItem(i);
      if (!stack.isEmpty() && texture.test(stack)) {
        // fetch texture from the block if it has one
        Block block = RetexturedHelper.getTexture(stack);
        // assuming it does not, use the block itself as the texture (provided it is not the result that is)
        if (block == Blocks.AIR && stack.getItem() != result.getItem()) {
          block = Block.byItem(stack.getItem());
        }
        // if no texture, skip
        if (block == Blocks.AIR) {
          continue;
        }

        // if we have not found a texture yet, store the found block
        if (currentTexture == null) {
          currentTexture = block;
          // match all means we must check the rest. If not match all, we can be done
          if (!matchAll) {
            break;
          }

          // if we found a texture before, must match or we do no texture
        } else if (currentTexture != block) {
          currentTexture = null;
          break;
        }
      }
    }

    // set the texture if found. No texture will use the fallback
    if (currentTexture != null) {
      return RetexturedHelper.setTexture(result, currentTexture);
    }
    return result;
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return MantleRecipes.CRAFTING_SHAPED_RETEXTURED.get();
  }

  public static class Serializer implements RecipeSerializer<ShapedRetexturedRecipe> {
    /** Single character key naming an ingredient in the recipe's key, matching what the pattern itself accepts */
    private static final Codec<Character> SYMBOL_CODEC = Codec.STRING.comapFlatMap(
      string -> string.length() == 1
                ? DataResult.success(string.charAt(0))
                : DataResult.error(() -> "Invalid texture key: '" + string + "' is an invalid symbol (must be 1 character only)."),
      String::valueOf);

    /** Intermediate form, needed as the recipe reads the pattern's key to resolve its texture but stores the resolved ingredient */
    private record Raw(String group, CraftingBookCategory category, ShapedRecipePattern.Data pattern, ItemStack result, boolean showNotification, char texture, boolean matchAll) {}

    private static final MapCodec<Raw> RAW_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      Codec.STRING.optionalFieldOf("group", "").forGetter(Raw::group),
      CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(Raw::category),
      ShapedRecipePattern.Data.MAP_CODEC.forGetter(Raw::pattern),
      ItemStack.STRICT_CODEC.fieldOf("result").forGetter(Raw::result),
      Codec.BOOL.optionalFieldOf("show_notification", Boolean.TRUE).forGetter(Raw::showNotification),
      SYMBOL_CODEC.fieldOf("texture").forGetter(Raw::texture),
      Codec.BOOL.optionalFieldOf("match_all", Boolean.FALSE).forGetter(Raw::matchAll)
    ).apply(instance, Raw::new));

    public static final MapCodec<ShapedRetexturedRecipe> CODEC = RAW_CODEC.flatXmap(Serializer::unpack, Serializer::pack);

    public static final StreamCodec<RegistryFriendlyByteBuf,ShapedRetexturedRecipe> STREAM_CODEC = StreamCodec.of(
      (buffer, recipe) -> {
        RecipeSerializer.SHAPED_RECIPE.streamCodec().encode(buffer, recipe);
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.texture);
        ByteBufCodecs.BOOL.encode(buffer, recipe.matchAll);
      },
      buffer -> {
        ShapedRecipe base = RecipeSerializer.SHAPED_RECIPE.streamCodec().decode(buffer);
        return new ShapedRetexturedRecipe(base, Ingredient.CONTENTS_STREAM_CODEC.decode(buffer), ByteBufCodecs.BOOL.decode(buffer));
      });

    /** Resolves the texture symbol against the key and builds the pattern */
    private static DataResult<ShapedRetexturedRecipe> unpack(Raw raw) {
      Ingredient texture = raw.pattern().key().get(raw.texture());
      if (texture == null) {
        return DataResult.error(() -> "Texture ingredient references symbol '" + raw.texture() + "' but it's not defined in the key");
      }
      ShapedRecipePattern pattern;
      try {
        pattern = ShapedRecipePattern.of(raw.pattern().key(), raw.pattern().pattern());
      } catch (IllegalStateException e) {
        return DataResult.error(e::getMessage);
      }
      return DataResult.success(new ShapedRetexturedRecipe(raw.group(), raw.category(), pattern, raw.result(), raw.showNotification(), raw.pattern(), raw.texture(), texture, raw.matchAll()));
    }

    /** Recovers the form the recipe was read from, which a recipe read from the network does not have */
    private static DataResult<Raw> pack(ShapedRetexturedRecipe recipe) {
      if (recipe.data == null) {
        return DataResult.error(() -> "Cannot encode unpacked recipe");
      }
      return DataResult.success(new Raw(recipe.getGroup(), recipe.category(), recipe.data, recipe.result, recipe.showNotification(), recipe.textureKey, recipe.matchAll));
    }

    @Override
    public MapCodec<ShapedRetexturedRecipe> codec() {
      return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf,ShapedRetexturedRecipe> streamCodec() {
      return STREAM_CODEC;
    }
  }
}
