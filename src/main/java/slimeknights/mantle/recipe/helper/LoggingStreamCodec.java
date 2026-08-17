package slimeknights.mantle.recipe.helper;

import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import slimeknights.mantle.Mantle;

/**
 * Stream codec wrapper that logs exceptions before rethrowing them, as otherwise a recipe sync failure is invisible.
 * @param owner  Object named in the log message, typically the recipe serializer
 * @param codec  Codec doing the actual work
 * @param <T>  Recipe class
 * @see LoggingRecipeSerializer
 */
public record LoggingStreamCodec<T>(Object owner, StreamCodec<RegistryFriendlyByteBuf,T> codec) implements StreamCodec<RegistryFriendlyByteBuf,T> {
  @Override
  public T decode(RegistryFriendlyByteBuf buffer) {
    try {
      return codec.decode(buffer);
    } catch (RuntimeException e) {
      String error = owner.getClass().getSimpleName() + ": Error reading recipe from packet";
      Mantle.logger.error("{}", error, e);
      throw new DecoderException(error + " - " + e.getMessage(), e);
    }
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, T recipe) {
    try {
      codec.encode(buffer, recipe);
    } catch (RuntimeException e) {
      String error = owner.getClass().getSimpleName() + ": Error writing recipe of class " + recipe.getClass().getSimpleName() + " to packet";
      Mantle.logger.error("{}", error, e);
      throw new EncoderException(error + " - " + e.getMessage(), e);
    }
  }
}
