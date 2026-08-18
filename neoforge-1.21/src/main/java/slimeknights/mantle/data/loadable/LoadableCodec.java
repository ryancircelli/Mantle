package slimeknights.mantle.data.loadable;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import slimeknights.mantle.Mantle;

/**
 * Implementation of a {@link Codec} using a {@link Loadable}.
 * <p>
 * The loadable reads and writes the caller's format directly, so no format conversion happens in either direction;
 * a loadable which has not implemented a format natively will convert internally, see
 * {@link Loadable#convert(DynamicOps, Object, String, slimeknights.mantle.util.typed.TypedMap)}.
 * @param <T>  Object type
 */
public record LoadableCodec<T>(Loadable<T> loadable) implements Codec<T> {
  /** Key used in error messages, as a codec has no notion of the field which contained the value */
  private static final String KEY = "codec";

  /**
   * {@inheritDoc}
   * @implNote  The whole input is consumed, so the remainder in the returned pair is {@link DynamicOps#empty()}.
   */
  @Override
  public <O> DataResult<Pair<T,O>> decode(DynamicOps<O> ops, O input) {
    return ErrorFactory
      .catching(() -> loadable.convert(ops, input, KEY), e -> Mantle.logger.warn("Unable to decode {}", loadable, e))
      .map(value -> Pair.of(value, ops.empty()));
  }

  /**
   * {@inheritDoc}
   * @implNote  A loadable writes a whole value rather than something appendable, so a non-empty prefix is an error.
   */
  @Override
  public <O> DataResult<O> encode(T input, DynamicOps<O> ops, O prefix) {
    return ErrorFactory
      .catching(() -> loadable.serialize(ops, input), e -> Mantle.logger.warn("Unable to encode {}", loadable, e))
      .flatMap(value -> ops.mergeToPrimitive(prefix, value));
  }

  @Override
  public String toString() {
    return "LoadableCodec[" + loadable + ']';
  }
}
