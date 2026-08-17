package slimeknights.mantle.recipe.helper;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Recipe serializer that logs network exceptions before throwing them as otherwise the exceptions may be invisible.
 * <p>
 * The reading and writing lives in {@link #streamCodecSafe()}; the logging is applied by {@link LoggingStreamCodec}.
 * @param <T>  Recipe class
 */
public interface LoggingRecipeSerializer<T extends Recipe<?>> extends RecipeSerializer<T> {
  /**
   * The codec doing the actual reading and writing, which {@link #streamCodec()} wraps in logging.
   * @return  Stream codec without logging
   */
  StreamCodec<RegistryFriendlyByteBuf,T> streamCodecSafe();

  /**
   * {@inheritDoc}
   * @implNote  Builds a fresh wrapper per call. An implementation syncing many recipes should cache
   *            {@code new LoggingStreamCodec<>(this, codec)} in a field and return it from both methods instead.
   */
  @Override
  default StreamCodec<RegistryFriendlyByteBuf,T> streamCodec() {
    return new LoggingStreamCodec<>(this, streamCodecSafe());
  }
}
