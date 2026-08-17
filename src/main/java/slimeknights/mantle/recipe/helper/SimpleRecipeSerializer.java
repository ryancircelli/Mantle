package slimeknights.mantle.recipe.helper;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.function.Supplier;

/**
 * Simple implementation of a recipe serializer for a recipe with no properties.
 * <p>
 * The recipe has no fields of its own, so this takes a supplier and the recipe JSON is an empty object.
 * @param <T>  Recipe class
 */
public class SimpleRecipeSerializer<T extends Recipe<?>> implements RecipeSerializer<T> {
  private final MapCodec<T> codec;
  private final StreamCodec<RegistryFriendlyByteBuf,T> streamCodec;

  public SimpleRecipeSerializer(Supplier<T> constructor) {
    this.codec = MapCodec.unit(constructor);
    this.streamCodec = StreamCodec.of((buffer, recipe) -> {}, buffer -> constructor.get());
  }

  @Override
  public MapCodec<T> codec() {
    return codec;
  }

  @Override
  public StreamCodec<RegistryFriendlyByteBuf,T> streamCodec() {
    return streamCodec;
  }
}
