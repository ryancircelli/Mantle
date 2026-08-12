package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * Loadable for ingredients, handling NeoForge's custom ingredient types.
 * @apiNote  This was bridged through gson in 1.20 as {@code Ingredient.fromJson} was a JSON only API. 1.21 replaced it
 *           with {@link Ingredient#CODEC}, which handles {@link net.neoforged.neoforge.common.crafting.ICustomIngredient}
 *           dispatch itself, so this now reads and writes the caller's own format.
 */
public enum IngredientLoadable implements Loadable<Ingredient> {
  ALLOW_EMPTY,
  DISALLOW_EMPTY;

  /** Gets the codec matching this loadable's empty handling */
  private Codec<Ingredient> ingredientCodec() {
    return this == ALLOW_EMPTY ? Ingredient.CODEC : Ingredient.CODEC_NONEMPTY;
  }

  @Override
  public Ingredient convert(JsonElement element, String key, TypedMap context) {
    return convert(JsonOps.INSTANCE, element, key, context);
  }

  @Override
  public <O> Ingredient convert(DynamicOps<O> ops, O input, String key, TypedMap context) {
    return ingredientCodec().parse(ops, input).getOrThrow(ErrorFactory.JSON_SYNTAX_ERROR::create);
  }

  @Override
  public JsonElement serialize(Ingredient object) {
    return serialize(JsonOps.INSTANCE, object);
  }

  @Override
  public <O> O serialize(DynamicOps<O> ops, Ingredient object) {
    if (object.isEmpty() && this == DISALLOW_EMPTY) {
      throw new IllegalArgumentException("Ingredient cannot be empty");
    }
    return ingredientCodec().encodeStart(ops, object).getOrThrow(ErrorFactory.RUNTIME::create);
  }

  @Override
  public Ingredient decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, Ingredient object) {
    Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, object);
  }
}
